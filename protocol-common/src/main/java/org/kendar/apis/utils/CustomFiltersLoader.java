package org.kendar.apis.utils;


import org.kendar.apis.FilterDescriptor;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface CustomFiltersLoader {
    List<FilterDescriptor> loadFilters();

    boolean handle(
            Request request,
            Response response)
            throws InvocationTargetException, IllegalAccessException;
}
