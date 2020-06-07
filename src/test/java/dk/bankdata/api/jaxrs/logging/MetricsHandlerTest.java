package dk.bankdata.api.jaxrs.logging;

import dk.bankdata.api.jaxrs.jwt.JwtToken;
import org.jose4j.jwt.JwtClaims;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MetricsHandlerTest {
    @InjectMocks @Spy MetricsHandler metricsHandler;

    @Test
    public void shouldDoMetricsConstructedCPR() {
        JwtClaims jwtClaims = mock(JwtClaims.class);
        when(jwtClaims.hasClaim("cpr")).thenReturn(false);

        JwtToken jwtToken = mock(JwtToken.class);
        when(jwtToken.getJwtClaims()).thenReturn(jwtClaims);

        metricsHandler.doMetrics(jwtToken, null);
    }

    @Test
    public void shouldDoMetricsCPR() {
        JwtClaims jwtClaims = mock(JwtClaims.class);
        when(jwtClaims.hasClaim("cpr")).thenReturn(true);
        when(jwtClaims.getClaimValue("cpr")).thenReturn("0303180192");

        JwtToken jwtToken = mock(JwtToken.class);
        when(jwtToken.getJwtClaims()).thenReturn(jwtClaims);

        metricsHandler.doMetrics(jwtToken, null);
    }

    @Test
    public void shouldCalculateAge() {
        String age = metricsHandler.getAge("2606754121");
        Assert.assertEquals(44, Integer.parseInt(age));
    }

    @Test
    public void shouldCalculateAge1900th() {
        String age = metricsHandler.getAge("0303180192");
        Assert.assertEquals(102, Integer.parseInt(age));
    }

    @Test
    public void shouldCalculateAge2020() {
        String age = metricsHandler.getAge("0303204482");
        Assert.assertEquals(0, Integer.parseInt(age));
    }

    @Test
    public void shouldHandleNullCprInAge() {
        String age = metricsHandler.getAge(null);
        Assert.assertEquals("Unknown", age);
    }

    @Test
    public void shouldFindMale() {
        String gender = metricsHandler.getGender("2606754121");
        Assert.assertEquals("Male", gender);
    }

    @Test
    public void shouldFindFemale() {
        String gender = metricsHandler.getGender("0303180192");
        Assert.assertEquals("Female", gender);
    }

    @Test
    public void shouldHandleNullCprInGender() {
        String gender = metricsHandler.getGender(null);
        Assert.assertEquals("Unknown", gender);
    }


}