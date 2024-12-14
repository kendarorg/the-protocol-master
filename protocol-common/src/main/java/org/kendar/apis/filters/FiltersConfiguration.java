package org.kendar.apis.filters;

import org.kendar.apis.FilterDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FiltersConfiguration {
    public final List<FilterDescriptor> filters = new ArrayList<>();
    public final HashMap<String, FilterDescriptor> filtersById = new HashMap<>();
    public final HashMap<String, List<FilterDescriptor>> filtersByClass = new HashMap<>();

    public FiltersConfiguration copy() {
        var result = new FiltersConfiguration();
        result.filters.addAll(filters);
        result.filtersById.putAll(filtersById);
        for (var item : filtersByClass.entrySet()) {
            result.filtersByClass.put(item.getKey(), new ArrayList<>(item.getValue()));
        }
        return result;
    }
}
