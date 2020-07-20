package io.jexxa.infrastructure.drivenadapterstrategy.messaging.jms;


import static io.jexxa.TestConstants.JEXXA_APPLICATION_SERVICE;
import static io.jexxa.TestConstants.JEXXA_DRIVEN_ADAPTER;
import static io.jexxa.infrastructure.drivenadapterstrategy.messaging.Queue.queueOf;
import static io.jexxa.infrastructure.drivenadapterstrategy.messaging.Topic.topicOf;
import static io.jexxa.infrastructure.utils.messaging.QueueListener.QUEUE_DESTINATION;
import static io.jexxa.infrastructure.utils.messaging.TopicListener.TOPIC_DESTINATION;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;

import com.google.gson.Gson;
import io.jexxa.TestConstants;
import io.jexxa.application.domain.valueobject.JexxaValueObject;
import io.jexxa.core.JexxaMain;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.Queue;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.Topic;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.MessageSender;
import io.jexxa.infrastructure.drivenadapterstrategy.messaging.MessageSenderManager;
import io.jexxa.infrastructure.drivingadapter.messaging.JMSAdapter;
import io.jexxa.infrastructure.utils.messaging.QueueListener;
import io.jexxa.infrastructure.utils.messaging.TopicListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Tag(TestConstants.INTEGRATION_TEST)
class JMSSenderIT
{
    private final JexxaValueObject message = new JexxaValueObject(42);

    private TopicListener topicListener;
    private QueueListener queueListener;
    private JexxaMain jexxaMain;

    private MessageSender objectUnderTest;

    @BeforeEach
    void initTests()
    {
        jexxaMain = new JexxaMain(JMSSenderIT.class.getSimpleName());
        topicListener = new TopicListener();
        queueListener = new QueueListener();
        objectUnderTest = MessageSenderManager.getInstance().getStrategy(jexxaMain.getProperties());

        jexxaMain.addToApplicationCore(JEXXA_APPLICATION_SERVICE)
                .addToInfrastructure(JEXXA_DRIVEN_ADAPTER)
                .bind(JMSAdapter.class).to(queueListener)
                .bind(JMSAdapter.class).to(topicListener)
                .start();
    }

    @Test
    void sendMessageToTopic()
    {
        //Arrange --

        //Act
        objectUnderTest.sendToTopic(message, TOPIC_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !topicListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageToTopicFluentAPI()
    {
        //Arrange --

        //Act
        objectUnderTest
                .send(message)
                .to(topicOf(TOPIC_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .asJson();

        //Act
        objectUnderTest
                .send(message)
                .to(topicOf(TOPIC_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .asJson();

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !topicListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageToTopicFluentAPI2()
    {
        //Arrange
        var gson = new Gson();

        //Act
        objectUnderTest
                .send(message)
                .to(topicOf(TOPIC_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .as(gson::toJson);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !topicListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageToQueue()
    {
        //Arrange --

        //Act
        objectUnderTest.sendToQueue(message, QUEUE_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !queueListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageToQueueFluentAPI()
    {
        //Arrange --

        //Act
        objectUnderTest
                .send(message)
                .to(queueOf(QUEUE_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .asJson();

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !queueListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }


    @Test
    void sendTextToQueueFluentAPI()
    {
        //Arrange --

        //Act
        objectUnderTest
                .send(message)
                .to(queueOf(QUEUE_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .asString();

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !queueListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendTextToTopicFluentAPI2()
    {
        //Arrange

        //Act
        objectUnderTest
                .send(message)
                .to(topicOf(TOPIC_DESTINATION))
                .addHeader("type", message.getClass().getSimpleName())
                .as(message::toString);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> !topicListener.getMessages().isEmpty());

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    @Test
    void sendMessageReconnectQueue() throws JMSException
    {
        //Arrange --

        //Act (simulate an error in between sending two messages
        objectUnderTest.sendToQueue(message, QUEUE_DESTINATION);
        simulateConnectionException(((JMSSender) (objectUnderTest)).getConnection());
        objectUnderTest.sendToQueue(message, QUEUE_DESTINATION);

        //Assert
        await().atMost(1, TimeUnit.SECONDS).until(() -> queueListener.getMessages().size() >= 2);

        assertTimeout(Duration.ofSeconds(1), jexxaMain::stop);
    }

    private void simulateConnectionException(Connection connection) throws JMSException
    {
        var listener = connection.getExceptionListener();

        connection.close();

        listener.onException(new JMSException("Simulated error "));
    }
}