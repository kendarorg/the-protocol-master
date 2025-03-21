package org.kendar.postgres.executor.converters;

import org.kendar.sql.jdbc.BindingParameter;
import org.kendar.sql.parser.SqlStringParser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PostgresCallConverter {
    public static final SqlStringParser parser = new SqlStringParser("$");
    private static final Pattern callToSelect = Pattern.compile("select \\* from ([a-zA-Z0-9_\\-]+)([\\s]*)\\((.*)\\)([\\s]*)as result", Pattern.CASE_INSENSITIVE);

    public static String convertToJdbc(String originalQuery, List<BindingParameter> parameterValues) {
        var matcher = callToSelect.matcher(originalQuery);
        if (!matcher.matches()) {
            return originalQuery;
        }
        var query = matcher.group(1).trim();
        var params = matcher.group(3).trim();
        var parsedParams = parser.parseString(params);
        var singleOut = parameterValues.stream().filter(BindingParameter::isOutput).count() == 1;
        if (singleOut) {
            query = "{? = call " + query + "(";
        } else {
            query = "{call " + query + "(";
        }
        var resultingParams = parsedParams.stream().
                map(String::trim).
                filter(pp -> (!pp.isEmpty() && !pp.equalsIgnoreCase(","))).
                toList();
        var finalParams = retrieveFinalParams(parameterValues, resultingParams, singleOut);
        query += String.join(",", finalParams) + ")}";
        return query;
    }

    private static ArrayList<String> retrieveFinalParams(List<BindingParameter> parameterValues, List<String> resultingParams, boolean singleOut) {
        var paramIndex = 0;
        var finalParams = new ArrayList<String>();
        for (String parPar : resultingParams) {
            if (!parPar.startsWith("$")) {
                finalParams.add(parPar);
                continue;
            }
            var binPar = parameterValues.get(paramIndex);
            if (binPar.isOutput()) {
                if (singleOut) {
                    paramIndex++;
                    continue;
                }

            }
            finalParams.add("?");
            paramIndex++;
        }
        return finalParams;
    }
}
