package org.kendar.annotations;

import io.swagger.v3.oas.models.OpenAPI;

public interface SwaggerEnricher {

    void enrich(OpenAPI swagger);
}
