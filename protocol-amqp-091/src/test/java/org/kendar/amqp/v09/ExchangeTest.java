package org.kendar.amqp.v09;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.*;
import org.kendar.amqp.v09.exchange.Queue;
import org.kendar.amqp.v09.exchange.Square;
import org.kendar.utils.Sleeper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ExchangeTest extends BasicTest {

    private static String QUEUE_NAME = "square";
    private static String EXCHANGE_NAME = "myExchange";
    private static String KEY_NAME = "key";

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        try {
            afterClassBase();
        } catch (Exception ex) {

        }
    }

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        beforeEachBase(testInfo);
    }

    @AfterEach
    public void afterEach() {
        afterEachBase();
    }

    @Test
    void testExchange() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        var cs = "amqp://localhost:" + FAKE_PORT;
        //cs = rabbitContainer.getConnectionString();
        Sleeper.sleep(100);

        connectionFactory.setUri(cs);
        connectionFactory.setPassword(rabbitContainer.getAdminPassword());

        System.out.println("-----------------------------");
        Queue queue = new Queue(connectionFactory);
        System.out.println("-----------------------------");
        queue.createExchangeQueue(QUEUE_NAME, EXCHANGE_NAME, "direct", KEY_NAME);
        var numbers = new ArrayList<String>();
        numbers.add("1");
        numbers.add("2");
        numbers.add("3");
        numbers.add("4");
        numbers.add("5");
        System.out.println("-----------------------------");
        numbers.forEach((n) -> queue.sendMessage(EXCHANGE_NAME, KEY_NAME, n));
        Square sq = new Square();
        sq.listenToMessage(connectionFactory);
        Sleeper.sleep(250);
        assertEquals(5, Square.results.size());
        assertTrue(Square.results.containsKey("Square of 1 is: 1"));
        assertTrue(Square.results.containsKey("Square of 2 is: 4"));
        assertTrue(Square.results.containsKey("Square of 3 is: 9"));
        assertTrue(Square.results.containsKey("Square of 4 is: 16"));
        assertTrue(Square.results.containsKey("Square of 5 is: 25"));
    }
}
