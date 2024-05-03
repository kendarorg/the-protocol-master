package org.kendar.mongo.dtos;

public enum MongoCommandType {
    ucAggregation,
    ucGeospatial,
    ucQueryAndWrite,
    ucQueryPlanCache,
    doAuth,
    doUserManagement,
    doRoleManagement,
    doReplication,
    doSharding,
    doSession,
    doAdmin,
    doDiagnostic,
    auditing,
    atlasSearch
}
