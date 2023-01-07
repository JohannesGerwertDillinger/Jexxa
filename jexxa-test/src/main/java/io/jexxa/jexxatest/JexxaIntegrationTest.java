package io.jexxa.jexxatest;

import io.jexxa.core.BoundedContext;
import io.jexxa.core.JexxaMain;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.MessageSender;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.MessageSenderManager;
import io.jexxa.infrastructure.drivingadapter.messaging.DefaultJMSConfiguration;
import io.jexxa.infrastructure.drivingadapter.messaging.JMSConfiguration;
import io.jexxa.jexxatest.integrationtest.rest.BoundedContextHandler;
import io.jexxa.jexxatest.integrationtest.rest.RESTFulRPCHandler;
import io.jexxa.jexxatest.integrationtest.rest.UnirestObjectMapper;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static io.jexxa.infrastructure.drivingadapter.messaging.JMSConfiguration.MessagingType.QUEUE;
import static io.jexxa.jexxatest.JexxaTest.loadJexxaTestProperties;
import static org.awaitility.Awaitility.await;

public class JexxaIntegrationTest
{
    private final Properties properties;
    final BoundedContextHandler boundedContextHandler;

    public JexxaIntegrationTest(Class<?> application)
    {
        var jexxaMain = new JexxaMain(application);
        jexxaMain.addProperties( loadJexxaTestProperties() );
        this.properties = jexxaMain.getProperties();

        boundedContextHandler = new BoundedContextHandler(properties, BoundedContext.class);

        Unirest.config().setObjectMapper(new UnirestObjectMapper());

        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(100, TimeUnit.MILLISECONDS)
                .ignoreException(UnirestException.class)
                .until(boundedContextHandler::isRunning);
    }

    public RESTFulRPCHandler getRESTFulRPCHandler(Class<?> endpoint)
    {
        return new RESTFulRPCHandler(properties, endpoint);
    }

    public BoundedContextHandler getBoundedContext()
    {
        return boundedContextHandler;
    }

    MessageSender getMessageSender()
    {
        return MessageSenderManager.getMessageSender(JexxaIntegrationTest.class,  getProperties());
    }

    JMSConfiguration getJMSListener()
    {
        return new DefaultJMSConfiguration( "TGEST", QUEUE);
    }

    public Properties getProperties() {
        return properties;
    }

    public void shutDown()
    {
        Unirest.shutDown();
    }
}
