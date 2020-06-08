package dk.bankdata.api.jaxrs.logging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MetricsFilterTest {
    @InjectMocks @Spy
    MetricsFilter metricsFilter;

    @Test
    @SuppressWarnings("LineLength")
    public void shouldDoMetricsNoCpr() {
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString("Authorization")).thenReturn("eyJraWQiOiItNjc2ODc3MTk0IiwieDV0IjoiZl82LU4tVDFRSWh2YjRwckgyWF96S2pGZFRjIiwiYWxnIjoiUlMyNTYifQ.eyJqdGkiOiJlNDI2MGMzYy03MGY5LTQ3MjctYTliMi04ZTg2NTdiNDc5NmIiLCJkZWxlZ2F0aW9uSWQiOiJkNzAyMTZiNS1iNTcyLTQwMGEtOWRlZS01ZTFlN2U5NjNmODEiLCJleHAiOjE1OTExMzA2NDAsIm5iZiI6MTU5MTA5NDY0MCwic2NvcGUiOiJkaWdpdGFsYmFua2luZyByZXBsYXlJZDoxNDdkZTE1Ny03ODc0LTRjNWItYTY4MS04NzA5YWI4MzEwYWYiLCJpc3MiOiJodHRwczovL3Rlc3QtYXV0aC5qeXNrZWJhbmsuZGsvb2lkYyIsInN1YiI6IkhPQjA1MTA1MDE2MDNKTlAiLCJhdWQiOlsicmVsYXRpb25zYmFuayIsIm1vYmlsYmFua19taWRkbGV3YXJlIl0sImlhdCI6MTU5MTA5NDY0MCwicHVycG9zZSI6ImFjY2Vzc190b2tlbiIsImNwciI6IjI4MDY5MDIxNzEiLCJsYW5ndWFnZSI6IkVOIiwiY3VzdG9tZXJOdW1iZXIiOiIwNTAxNjAzIiwiYWNyIjoiYmFua2RhdGFfc3R1YmxvZ2luIiwiY3VzdG9tZXJUeXBlIjoiUCIsInRwcCI6ZmFsc2UsImJhbmtubyI6NTEsInVzZXJOdW1iZXIiOiJKTlAifQ.QZL4u2hYH5EoviXJXw64tuPsbr_NbvOebaCcF2xxUlsR-bsteij-XQKurwgBZyfrQV-l3pZQWtwQ3lcmMbT8Uj7kWBblJM-iuA-zNr_4IrZLqNhIZ6F1Kl1SHN_tb0xJoSHpUrBVGsM65LEFhpgnw0yZeL3sIBkr4UnnlEYbcIcF8YjcOlSdLRP1R8jqyqhPasiEiRaXzjbDI0gHKphbD5005QTbKMMN8Z4M7t6JzI-z9CvbRLvo4cwS3Met28rleHFyvsTM2wJb2M18I0cPIezfdIYiZ0qpZAOWjvlPIz5_Dsy-eHuLTHByAqDSjpHBNsR7aYmDK3VMxpgPxGJcdw");

        metricsFilter.filter(containerRequestContext);
    }

    @Test
    @SuppressWarnings("LineLength")
    public void shouldDoMetricsCpr() {
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString("Authorization")).thenReturn("eyJraWQiOiItNjc2ODc3MTk0IiwieDV0IjoiZl82LU4tVDFRSWh2YjRwckgyWF96S2pGZFRjIiwiYWxnIjoiUlMyNTYifQ.eyJqdGkiOiJlNDI2MGMzYy03MGY5LTQ3MjctYTliMi04ZTg2NTdiNDc5NmIiLCJkZWxlZ2F0aW9uSWQiOiJkNzAyMTZiNS1iNTcyLTQwMGEtOWRlZS01ZTFlN2U5NjNmODEiLCJleHAiOjE1OTExMzA2NDAsIm5iZiI6MTU5MTA5NDY0MCwic2NvcGUiOiJkaWdpdGFsYmFua2luZyByZXBsYXlJZDoxNDdkZTE1Ny03ODc0LTRjNWItYTY4MS04NzA5YWI4MzEwYWYiLCJpc3MiOiJodHRwczovL3Rlc3QtYXV0aC5qeXNrZWJhbmsuZGsvb2lkYyIsInN1YiI6IkhPQjA1MTA1MDE2MDNKTlAiLCJhdWQiOlsicmVsYXRpb25zYmFuayIsIm1vYmlsYmFua19taWRkbGV3YXJlIl0sImlhdCI6MTU5MTA5NDY0MCwicHVycG9zZSI6ImFjY2Vzc190b2tlbiIsImNwciI6IjI4MDY5MDIxNzEiLCJsYW5ndWFnZSI6IkVOIiwiY3VzdG9tZXJOdW1iZXIiOiIwNTAxNjAzIiwiYWNyIjoiYmFua2RhdGFfc3R1YmxvZ2luIiwiY3VzdG9tZXJUeXBlIjoiUCIsInRwcCI6ZmFsc2UsImJhbmtubyI6NTEsInVzZXJOdW1iZXIiOiJKTlAifQ.QZL4u2hYH5EoviXJXw64tuPsbr_NbvOebaCcF2xxUlsR-bsteij-XQKurwgBZyfrQV-l3pZQWtwQ3lcmMbT8Uj7kWBblJM-iuA-zNr_4IrZLqNhIZ6F1Kl1SHN_tb0xJoSHpUrBVGsM65LEFhpgnw0yZeL3sIBkr4UnnlEYbcIcF8YjcOlSdLRP1R8jqyqhPasiEiRaXzjbDI0gHKphbD5005QTbKMMN8Z4M7t6JzI-z9CvbRLvo4cwS3Met28rleHFyvsTM2wJb2M18I0cPIezfdIYiZ0qpZAOWjvlPIz5_Dsy-eHuLTHByAqDSjpHBNsR7aYmDK3VMxpgPxGJcdw");

        metricsFilter.filter(containerRequestContext);
    }

    @Test
    public void shouldCalculateAge() {
        String age = metricsFilter.getAge("2606754121");
        Assert.assertEquals(44, Integer.parseInt(age));
    }

    @Test
    public void shouldCalculateAge1900th() {
        String age = metricsFilter.getAge("0303180192");
        Assert.assertEquals(102, Integer.parseInt(age));
    }

    @Test
    public void shouldCalculateAge2020() {
        String age = metricsFilter.getAge("0303204482");
        Assert.assertEquals(0, Integer.parseInt(age));
    }

    @Test
    public void shouldHandleNullCprInAge() {
        String age = metricsFilter.getAge(null);
        Assert.assertEquals("Unknown", age);
    }

    @Test
    public void shouldFindMale() {
        String gender = metricsFilter.getGender("2606754121");
        Assert.assertEquals("Male", gender);
    }

    @Test
    public void shouldFindFemale() {
        String gender = metricsFilter.getGender("0303180192");
        Assert.assertEquals("Female", gender);
    }

    @Test
    public void shouldHandleNullCprInGender() {
        String gender = metricsFilter.getGender(null);
        Assert.assertEquals("Unknown", gender);
    }
}