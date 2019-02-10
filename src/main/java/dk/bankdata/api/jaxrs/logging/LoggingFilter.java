package dk.bankdata.api.jaxrs.logging;

import static dk.bankdata.api.jaxrs.logging.Logging.LogEnabled;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Log request and response statuscode, entity and process timer.
 *
 * <p>
 *     Each endpoint that is annotated with LogEnabled will have its input and output logged.
 *     Request entity (if present) along with response entity (if present) and the process time
 *     will be added to the log.
 *
 *     The request and response entity has a max length of 197 characters followed by ... to limit
 *     log size.
 *
 *     If a JWT is present in the header of the request, then to subject of the JWT will be logged
 *     to help track user interaction.
 *
 *     The log is limited to logging only if LOG.isDebugEnabled() is true
 *
 *     Example of an endpoint using the annotation.
 * </p>
 *
 * <code>
 *     GET
 *     Path("/data)
 *     &#xA9;LogEnabled
 *     &#xA9;Consumes(MediaType.APPLICATION_JSON)
 *     &#xA9;Produces(MediaType.APPLICATION_JSON)
 *     public Response getData() {
 *        ...
 *     }
 * </code>
 */
@LogEnabled
@Provider
@Priority(Priorities.USER - 99)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String KEY_SUBJECT = "subject";
    private static final String KEY_EXECUTION_TIME = "Execution-Time";
    private static final String KEY_HTTP_STATUS = "http-status";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!LOG.isDebugEnabled()) return;

        requestContext.setProperty("request-timer", System.currentTimeMillis());

        String entity = "";

        if (requestContext.hasEntity()) {
            byte[] entityData = toByteArray(requestContext.getEntityStream());
            requestContext.setEntityStream(new ByteArrayInputStream(entityData));
            entity = truncate(entity);
        }

        requestContext.setProperty("request-entity", entity);
        String jwt = requestContext.getHeaderString("Authorization");

        if (jwt != null) {
            try {
                String pureJwt = jwt.replace("Bearer ", "");
                JWT jwtObject = JWTParser.parse(pureJwt);
                JWTClaimsSet jwtClaimsSet = jwtObject.getJWTClaimsSet();

                String jwtSubject = jwtClaimsSet.getSubject();

                MDC.put(KEY_SUBJECT, jwtSubject);

            } catch (ParseException e) {
                LOG.error("LoggingFilter failed with message {} ", e.getMessage());
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        if (!LOG.isDebugEnabled()) return;

        long executionTime = getExecutionTime(requestContext);
        MDC.put(KEY_EXECUTION_TIME, String.valueOf(executionTime));

        int httpStatus = responseContext.getStatus();
        MDC.put(KEY_HTTP_STATUS, String.valueOf(httpStatus));

        String requestEntity = (String) requestContext.getProperty("request-entity");
        String responseEntity = truncate(getResponseEntity(responseContext));
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        LOG.debug("method={}, path={}, request={}, status={}, time={} ms, response={}",
                method, path, requestEntity, httpStatus, executionTime, responseEntity);

        MDC.remove(KEY_SUBJECT);
        MDC.remove(KEY_EXECUTION_TIME);
        MDC.remove(KEY_HTTP_STATUS);
    }

    private String getResponseEntity(ContainerResponseContext responseContext) {
        String responseEntity = "";

        if (responseContext.hasEntity()) {
            Object entity = responseContext.getEntity();
            responseEntity = entity.toString();
        }

        return responseEntity;
    }

    private long getExecutionTime(ContainerRequestContext requestContext) {
        Long startTime = (Long) requestContext.getProperty("request-timer");
        long executionTime = -1;
        if (startTime != null) {
            executionTime = System.currentTimeMillis() - startTime;
        }

        return executionTime;
    }

    private static byte[] toByteArray(InputStream input) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (BufferedInputStream inputStream = new BufferedInputStream(input)) {
            int b;
            while ((b = inputStream.read()) != -1) {
                outputStream.write(b);
            }
        } catch (IOException e) {
            LOG.error("Unable to read request entity", e);
        }

        return outputStream.toByteArray();
    }

    private static String truncate(String data) {
        return data != null && data.length() > 200 ? data.substring(0, 197) + "..." : data;
    }
}

