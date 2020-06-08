package dk.bankdata.api.jaxrs.logging;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MetricsFilter implements ContainerRequestFilter {
    private static final Logger LOG = LoggerFactory.getLogger(MetricsFilter.class);

    public void filter(ContainerRequestContext requestContext) {
        String jwt = requestContext.getHeaderString("Authorization");

        if (jwt != null) {
            try {
                String pureJwt = jwt.replace("Bearer ", "");

                JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                        .setSkipAllValidators()
                        .setSkipSignatureVerification()
                        .setSkipVerificationKeyResolutionOnNone()
                        .build();

                JwtClaims jwtClaims = jwtConsumer.processToClaims(pureJwt);

                String cpr = jwtClaims.hasClaim("cpr")
                        ? String.valueOf(jwtClaims.getClaimValue("cpr"))
                        : null;

                String age = getAge(cpr);
                String gender = getGender(cpr);
                String userAgent = requestContext.getHeaderString("user-agent");

            } catch (Exception e) {
                String causeDetails = e.getCause() != null ?  e.getCause().getMessage() : "";
                String message = "Unable to doMetrics. Error encountered. " +
                        "Error was " + e.getMessage() + " / " + causeDetails;

                LOG.warn(message);
            }

        }

    }

    protected String getGender(String cpr) {
        if (cpr == null) {
            return "Unknown";
        }

        return Integer.parseInt(cpr.substring(5, 6)) % 2 == 0 ? "Female" : "Male";
    }

    protected String getAge(String cpr) {
        if (cpr == null) {
            return "Unknown";
        }

        String day = cpr.substring(0, 2);
        String month = cpr.substring(2, 4);
        int year = Integer.parseInt(cpr.substring(4, 6));
        int century = 0;

        char seventhNumber = cpr.charAt(6);

        switch (seventhNumber) {
            case '0':
            case '1':
            case '2':
            case '3':
                century = 1900;
                break;
            case '4':
            case '9':
                century = year < 37 ? 2000 : 1900;
                break;
            default: // 5, 6, 7, 8
                century = year < 37 ? 2000 : year > 57 ? 1800 : 0;
        }

        year += century;

        String strDate = day + month + year;

        LocalDate localDate = LocalDate.parse(strDate, DateTimeFormatter.ofPattern("ddMMyyyy"));
        LocalDate now = LocalDate.now();

        return String.valueOf(Period.between(localDate, now).getYears());
    }
}
