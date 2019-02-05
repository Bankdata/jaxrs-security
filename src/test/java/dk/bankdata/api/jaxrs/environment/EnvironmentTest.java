package dk.bankdata.api.jaxrs.environment;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentTest {
    @Test
    public void shouldCreateEnvironment() {
        Environment environment = new Environment("some-issuer-1,some-issuer-2", "some-proxy", "some-cipher");

        Assert.assertEquals(environment.getCipherKey(), "some-cipher");
        Assert.assertEquals(environment.getProxyUrl(), "some-proxy");
        Assert.assertEquals(environment.getIssuers().size(), 2);
        Assert.assertEquals(environment.getIssuers().get(0), "some-issuer-1");
        Assert.assertEquals(environment.getIssuers().get(1), "some-issuer-2");
    }
}