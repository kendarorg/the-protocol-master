package org.kendar.mysql;

import org.junit.jupiter.api.*;
import org.kendar.mysql.jpa.CompanyJpa;
import org.kendar.tests.jpa.HibernateSessionFactory;
import org.kendar.utils.Sleeper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JpaTest extends MySqlBasicTest {
    @BeforeAll
    public static void beforeClass() {
        beforeClassBase();

    }

    @AfterAll
    public static void afterClass() throws Exception {
        afterClassBase();
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
    void simpleJpaTest() throws Exception {
        Sleeper.sleep(5000, () -> protocolServer.isRunning());
        HibernateSessionFactory.initialize("com.mysql.cj.jdbc.Driver",
                String.format("jdbc:mysql://127.0.0.1:%d", FAKE_PORT),
                mysqlContainer.getUserId(), mysqlContainer.getPassword(),
                "org.hibernate.dialect.MySQL8Dialect",
                CompanyJpa.class);


        HibernateSessionFactory.transactional(em -> {
            var lt = new CompanyJpa();
            lt.setDenomination("Test Ltd");
            lt.setAddress("TEST RD");
            lt.setAge(22);
            lt.setSalary(500.22);
            em.persist(lt);
        });
        var atomicBoolean = new AtomicBoolean(false);
        HibernateSessionFactory.query(em -> {
            var resultset = em.createQuery("SELECT denomination FROM CompanyJpa").getResultList();
            for (var rss : resultset) {
                assertEquals("Test Ltd", rss);
                atomicBoolean.set(true);
            }
        });

        assertTrue(atomicBoolean.get());

        var events = getEvents().stream().collect(Collectors.toList());
        assertTrue(events.size() >= 7);
        var evt = events.get(0);
        assertEquals("mysql", evt.getProtocol());
    }
}
