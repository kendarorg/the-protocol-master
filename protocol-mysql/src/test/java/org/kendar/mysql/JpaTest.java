package org.kendar.mysql;

import org.junit.jupiter.api.*;
import org.kendar.jpa.HibernateSessionFactory;
import org.kendar.mysql.jpa.CompanyJpa;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JpaTest extends BasicTest {
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
        HibernateSessionFactory.initialize("com.mysql.cj.jdbc.Driver",
                //postgresContainer.getJdbcUrl(),
                String.format("jdbc:mysql://127.0.0.1:%d", FAKE_PORT),
                mysqlContainer.getUserId(), mysqlContainer.getPassword(),
                "org.hibernate.dialect.MySQLDialect",
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


    }
}
