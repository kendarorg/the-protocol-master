package org.kendar.http.utils.dns;

import java.util.Calendar;
import java.util.HashSet;

public class ResolvedDomain {
    public final HashSet<String> domains = new HashSet<>();
    public final long timestamp = Calendar.getInstance().getTimeInMillis();
}