package org.kendar.jpa;

import javax.persistence.EntityManager;

@FunctionalInterface
public interface EntityManagerFunctionResult {
    Object apply(EntityManager em) throws Exception;
}
