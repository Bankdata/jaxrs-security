package dk.bankdata.api.jaxrs.logging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import javax.annotation.security.PermitAll;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingFilterTest {
    @InjectMocks
    private LoggingFilter loggingFilter;

    @Mock
    private ResourceInfo resourceInfo;

    @Test
    public void shouldParseJwt() throws Exception {
        String jwt = "eyJraWQiOiItNjc2ODc3MTk0IiwieDV0IjoiZl82LU4tVDFRSWh2YjRwckgyWF96S2pGZFRjIiwiYWxnIjoiUlMyNTY" +
                "ifQ.eyJiYW5rbm8iOjUxLCJjcHIiOiIzMTEyNjEwNzE0IiwiY3VzdG9tZXJOdW1iZXIiOiI1MDEzNjgiLCJ0cHAiOmZhbHNl" +
                "LCJ1c2VyTnVtYmVyIjoiVFJIIiwianRpIjoiNzg0YTk1YjItMDJiMC00NmE5LTk4MzktYjQyNmExZGZiMzEyIiwiZGVsZWdh" +
                "dGlvbklkIjoiNWZjNmJiODAtZmExZC00MjliLTgxZWUtYmVhZmMyNTVhNzk0IiwiZXhwIjoxNTU3NzY2MjA2LCJuYmYiOjE1" +
                "NTc3MzAyMDYsInNjb3BlIjoib3BlbmlkIGRpZ2l0YWxiYW5raW5nIiwiaXNzIjoiaHR0cHM6Ly90ZXN0LWF1dGguanlza2Vi" +
                "YW5rLmRrL29pZGMiLCJzdWIiOiJIT0IwNTEwNTAxMzY4VFJIIiwiYXVkIjpbInJlbGF0aW9uc2JhbmsiLCJtb2JpbGJhbmtf" +
                "bWlkZGxld2FyZSJdLCJpYXQiOjE1NTc3MzAyMDYsInB1cnBvc2UiOiJhY2Nlc3NfdG9rZW4ifQ.HuxSTwFkerrqBMP6QxZcL" +
                "XpTtJDRPD_t5ckouYX5eYwpfsheVjJdstcPsRNXpEujo6sWIeZFkFFXe0uedkz8lp0T-nGyZBDmV-WwwvjQA0PBhVVhp477G" +
                "5n0FZ5LrGtdR3sSmY9d1OD1Kkax1EL-UfgjK28D7PKVACn0SQI_cCrWwurklzzniME2R3KGVpNXxbkRCOQRzpgwZwqz4V80k" +
                "zOLSharPFF3AsdyGTCDFkdGaoKBPSefOypQD6oKXNB-Z83WSy51cZq1z6-78KrZhr0cEz5MqWRnfRSjRAx8iqIftaQ93cBhk" +
                "mXNrmCYrACG05NsME8bFE2MZgzjpo1xHA";

        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        when(containerRequestContext.getHeaderString("Authorization")).thenReturn(jwt);

        Method method = MockClass.class.getDeclaredMethod("someTestFunction");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        loggingFilter.filter(containerRequestContext);

    }

    @Test
    public void shouldNotValidateJwt() throws Exception {
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);

        Method method = MockClass.class.getDeclaredMethod("someFunction");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        loggingFilter.filter(containerRequestContext);
    }

    private static class MockClass {
        @PermitAll()
        public void someFunction() {}

        public void someTestFunction() {}
    }
}