package org.kendar.tests.jpa;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.Query;
import java.util.List;
import java.util.Optional;

public class HibernateSessionFactory {

    private static final Object syncObject = new Object();
    private static Class<?>[] dbTablesList;
    private static SessionFactory sessionFactory;
    private static Configuration configuration;

    public static void initialize(String driver, String url, String login,
                                  String password, String dialect, Class<?>... dbTableList) {

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        sessionFactory = null;
        dbTablesList = new Class[]{};
        configuration = new Configuration();

        configuration.setProperty("hibernate.connection.driver_class", driver);
        configuration.setProperty("hibernate.connection.url", url);
        configuration.setProperty("hibernate.connection.username", login);
        configuration.setProperty("hibernate.connection.password", password);
        configuration.setProperty("hibernate.dialect", dialect);
        configuration.setProperty("show_sql", "true");
        configuration.setProperty("hibernate.hbm2ddl.auto", "create");
        dbTablesList = dbTableList;
        for (var table : dbTableList) {
            configuration.addAnnotatedClass(table);
        }
    }

    public static SessionFactory buildSessionFactory() throws HibernateException {
        if (sessionFactory != null) return sessionFactory;
        synchronized (syncObject) {
            if (sessionFactory == null) {
                sessionFactory = configuration.buildSessionFactory();
            }
        }
        return sessionFactory;
    }


    public static <T> T transactionalResult(EntityManagerFunctionResult function) throws Exception {
        var sessionFactory = buildSessionFactory();

        var em = sessionFactory.createEntityManager();
        em.getTransaction().begin();
        T result = (T) function.apply(em);
        em.getTransaction().commit();
        em.close();
        return result;
    }


    public static <T> T queryResult(EntityManagerFunctionResult function) throws Exception {
        var sessionFactory = buildSessionFactory();
        var em = sessionFactory.createEntityManager();
        T result = (T) function.apply(em);
        em.close();
        return result;
    }


    @SuppressWarnings("rawtypes")
    public static <T> Optional<T> querySingle(EntityManagerFunctionResult function) throws Exception {
        var sessionFactory = buildSessionFactory();
        var em = sessionFactory.createEntityManager();
        var query = (Query) function.apply(em);
        Optional result;
        var list = (List<T>) query.getResultList();
        if (list.isEmpty()) {
            em.close();
            result = Optional.empty();
        } else {
            result = Optional.of(list.get(0));
            em.close();
        }
        return result;
    }


    public static void transactional(EntityManagerFunction function) throws Exception {
        var sessionFactory = buildSessionFactory();
        var em = sessionFactory.createEntityManager();
        em.getTransaction().begin();
        function.apply(em);
        em.getTransaction().commit();
        em.close();
    }


    public static void query(EntityManagerFunction function) throws Exception {
        var sessionFactory = buildSessionFactory();
        var em = sessionFactory.createEntityManager();
        function.apply(em);
        em.close();
    }

    public static Class<?>[] getDbTablesList() {
        return dbTablesList;
    }
}
