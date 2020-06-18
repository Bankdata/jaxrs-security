package dk.bankdata.api.jaxrs.logging;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Log request and response statuscode, entity and process timer.
 *
 * <p>Each endpoint that is annotated with LogEnabled will have its input and output logged.
 * Request entity (if present) along with response entity (if present) and the process time
 * will be added to the log.
 *
 * <p>The request and response entity has a max length of 197 characters followed by ... to limit
 * log size.
 *
 * <p>If a JWT is present in the header of the request, then to subject of the JWT will be logged
 * to help track user interaction.
 *
 * <p>The log is limited to logging only if LOG.isDebugEnabled() is true
 * 
 * <p>Example of an endpoint using the annotation.
 * </p>
 *
 * <pre>
 *     GET
 *     Path("/data)
 *     &#xA9;LogEnabled
 *     &#xA9;Consumes(MediaType.APPLICATION_JSON)
 *     &#xA9;Produces(MediaType.APPLICATION_JSON)
 *     public Response getData() {
 *        ...
 *     }
 * </pre>
 */
@LogEnabled
@Provider
@Priority(Priorities.AUTHENTICATION - 1)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String KEY_EXECUTION_TIME = "Execution-Time";
    private static final String KEY_HTTP_STATUS = "http-status";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty("request-timer", System.currentTimeMillis());

        String jwt = requestContext.getHeaderString("Authorization");

        if (jwt != null && (jwt.length() - jwt.replace(".", "").length() == 2)) {
            try {
                String pureJwt = jwt.replace("Bearer ", "");

                JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setSkipSignatureVerification()
                        .setSkipVerificationKeyResolutionOnNone()
                        .build();

                JwtClaims jwtClaims = jwtConsumer.processToClaims(pureJwt);

                if (jwtClaims.hasClaim("bankno")) {
                    String bankno = jwtClaims.getClaimValue("bankno").toString();
                    requestContext.setProperty("bankno", bankno);
                }

            } catch (InvalidJwtException e) {
                String details = e.getMessage() + "." +
                        (e.getCause() != null ?  " Cause : " + e.getCause().getMessage() : "");

                LOG.error("LoggingFilter failed with message {} ", details);
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        long executionTime = getExecutionTime(requestContext);
        MDC.put(KEY_EXECUTION_TIME, String.valueOf(executionTime));

        int httpStatus = responseContext.getStatus();
        MDC.put(KEY_HTTP_STATUS, String.valueOf(httpStatus));

        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();
        String bankNo = (String) requestContext.getProperty("bankno");

        if (httpStatus >= 500) {
            LOG.error("method={}, path={}, bankno={}, status={}, time={} ms",
                    method, path, bankNo, httpStatus, executionTime);
        } else if (httpStatus >= 400) {
            LOG.warn("method={}, path={}, bankno={}, status={}, time={} ms",
                    method, path, bankNo, httpStatus, executionTime);
        } else {
            LOG.debug("method={}, path={}, bankno={}, status={}, time={} ms",
                    method, path, bankNo, httpStatus, executionTime);
        }

        MDC.remove(KEY_EXECUTION_TIME);
        MDC.remove(KEY_HTTP_STATUS);
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
            String details = e.getMessage() + "." +
                    (e.getCause() != null ?  " Cause : " + e.getCause().getMessage() : "");

            LOG.error("Unable to read request entity. Error was " + details);
        }

        return outputStream.toByteArray();
    }

    private static String truncate(String data) {
        return data != null && data.length() > 200 ? data.substring(0, 197) + "..." : data;
    }
}

