package dk.bankdata.api.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


/**
 * Filer to log input/output as well as key data from endpoint invocations.
 */
@LoggingInterface.Logging
@Provider
@ApplicationScoped
@Priority(Priorities.USER - 99)
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private static final String KEY_HOB_CUSTOMER = "HOBCUSTOMER";
    private static final String KEY_EXECUTION_TIME = "Execution-Time";
    private static final String KEY_HTTP_STATUS = "http-status";
    private static final String KEY_BANK = "bank";
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final ObjectReader READER = new ObjectMapper().reader();
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    @Context ResourceInfo resourceInfo;

    static {
        FIELDS.put("cprNr", input -> input.replaceAll("(?<=.{6}).", "*"));
        FIELDS.put("email", input -> input.replaceAll("(?<=.{2}).(?=[^@]*?@)", "*"));
        FIELDS.put("phoneNumber", input -> input.replaceAll("(?<=.{3}).", "*"));
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty("request-timer", System.currentTimeMillis());

        String entity = "";

        if (requestContext.hasEntity()) {
            byte[] entityData = toByteArray(requestContext.getEntityStream());
            requestContext.setEntityStream(new ByteArrayInputStream(entityData));
            entity = sanitizeEntity(new String(entityData, StandardCharsets.UTF_8));
            entity = truncate(entity);
        }

        requestContext.setProperty("request-entity", entity);
        String jwt = requestContext.getHeaderString("Authorization");

        if (jwt != null) {
            try {
                String pureJwt = jwt.replace("Bearer ", "");
                JWT jwtObject = JWTParser.parse(pureJwt);
                JWTClaimsSet jwtClaimsSet = jwtObject.getJWTClaimsSet();

                String hobCustomer = jwtClaimsSet.getSubject();
                int bankNo = Integer.valueOf(hobCustomer.substring(3, 6));

                MDC.put(KEY_HOB_CUSTOMER, hobCustomer);
                MDC.put(KEY_BANK, Integer.toString(bankNo));

            } catch (ParseException e) {
                LOG.error("Failed to log sanitizeEntity failed with message {} ", e.getMessage());
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        long executionTime = getExecutionTime(requestContext);
        MDC.put(KEY_EXECUTION_TIME, String.valueOf(executionTime));

        int httpStatus = responseContext.getStatus();
        MDC.put(KEY_HTTP_STATUS, String.valueOf(httpStatus));

        String auth = requestContext.getHeaderString("Authorization");
        String requestEntity = (String) requestContext.getProperty("request-entity");
        String responseEntity = truncate(getResponseEntity(responseContext));
        String method = requestContext.getMethod();
        String path = requestContext.getUriInfo().getPath();

        LOG.info("method={}, path={}, auth={}, request={}, status={}, time={} ms, response={}",
                method, path, auth, requestEntity, httpStatus, executionTime, responseEntity);

        MDC.remove(KEY_HOB_CUSTOMER);
        MDC.remove(KEY_BANK);
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

    String sanitizeEntity(String json) {
        try {
            ObjectNode root = (ObjectNode) READER.readTree(json);

            FIELDS.forEach((fieldName, rewriter) -> {
                JsonNode node = root.path(fieldName);

                if (!node.isMissingNode() && !node.isNull()) {
                    root.put(fieldName, rewriter.rewrite(node.asText()));
                }
            });

            return WRITER.writeValueAsString(root);

        } catch (Exception e) {
            LOG.warn("sanitizeEntity failed with message {} ", e.getMessage());
            return json;
        }
    }

    /**
     * Utility method to read input stream into byte array.
     */
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

    interface Field {
        String rewrite(String input);
    }
}

