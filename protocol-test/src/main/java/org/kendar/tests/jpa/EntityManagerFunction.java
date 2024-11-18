package org.kendar.tests.jpa;

import javax.persistence.EntityManager;

@FunctionalInterface
public interface EntityManagerFunction {
    void apply(EntityManager em) throws Exception;
}
