package org.kendar.annotations;

import io.swagger.v3.oas.models.OpenAPI;

/**
 * Interface to add extra specifications to swagger definitions
 */
public interface SwaggerEnricher {

    void enrich(OpenAPI swagger);
}
