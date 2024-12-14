package org.kendar.amqp.v09;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.*;
import org.kendar.amqp.v09.exchange.Queue;
import org.kendar.amqp.v09.exchange.Square;
import org.kendar.utils.Sleeper;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ExchangeTest extends BasicTest {

    private static final String QUEUE_NAME = "square";
    private static final String EXCHANGE_NAME = "myExchange";
    private static final String KEY_NAME = "key";

    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() {
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
    void testExchange() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
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

        Square sq = new Square();
        sq.listenToMessage(connectionFactory);
        Sleeper.sleep(100);
        numbers.forEach((n) -> queue.sendMessage(EXCHANGE_NAME, KEY_NAME, n));

        Sleeper.sleep(5000, () -> Square.results.size() == 5);
        assertTrue(Square.results.containsKey("Square of 1 is: 1"));
        assertTrue(Square.results.containsKey("Square of 2 is: 4"));
        assertTrue(Square.results.containsKey("Square of 3 is: 9"));
        assertTrue(Square.results.containsKey("Square of 4 is: 16"));
        assertTrue(Square.results.containsKey("Square of 5 is: 25"));
        var events = getEvents().stream().collect(Collectors.toList());
        assertEquals(7, events.size());
        assertEquals(2, events.stream().filter(e -> e.getQuery().equalsIgnoreCase("CONNECT")).count());
    }
}
