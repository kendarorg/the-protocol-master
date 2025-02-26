package org.kendar.runner;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import org.junit.jupiter.api.Test;
import org.kendar.events.ReportDataEvent;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ReportPluginTest {
    private JsonMapper mapper = new JsonMapper();
    String toTest = "{\n" +
            "  \"instanceId\": \"dns-01\",\n" +
            "  \"protocol\": \"dns\",\n" +
            "  \"query\": \"response\",\n" +
            "  \"connectionId\": 2,\n" +
            "  \"timestamp\": 1740489850092,\n" +
            "  \"duration\": 93,\n" +
            "  \"tags\": {\n" +
            "    \"ips\": \"160.153.0.79\",\n" +
            "    \"requestedDomain\": \"www.google.com\"\n" +
            "  }\n" +
            "}";



    @Test
    void testSimple() {
        var jp = new String("$@.protocol=='http'");

        List<Map<String, Object>> result= JsonPath.compile(jp, new Predicate[]{}).read(toTest);
        assertEquals(1,result.size());
        var data = mapper.deserialize(mapper.serialize(result.get(0)), ReportDataEvent.class);
        assertEquals("http-01",data.getInstanceId());

    }

    @Test
    void testOr() {
        var jp = new String("$[?(@.protocol=='http' || @.protocol=~/.*ns/i)]");

        List<Map<String, Object>> result= JsonPath.compile(jp, new Predicate[]{}).read(toTest);
        assertEquals(2,result.size());
        var data = mapper.deserialize(mapper.serialize(result.get(0)), ReportDataEvent.class);
        assertEquals("dns-01",data.getInstanceId());
        data = mapper.deserialize(mapper.serialize(result.get(1)), ReportDataEvent.class);
        assertEquals("http-01",data.getInstanceId());
    }

    @Test
    void testSub() {
        //var jp = new String("$[?(@.tags");
        //var jp = new String("$[?(@.tags[*].requestedDomain=~/.*kendar/i)]");
        //var jp = new String("$..[?(@.tags[?(@.requestedDomain=~/.*kendar/i)])]");
        //var jp = new String("$.tags[?(@.requestedDomain=~/.*kendar/i)])]");
        //var jp = new String("$[?(@.tags[*].requestedDomain=='www.kendar.com')]");
        //var jp = new String("$..tags[?(@.requestedDomain=='www.kendar.com')]"); //return ips+requestedDomain
        //var jp = new String("$.[?(@.tags[?(@.requestedDomain=~/.*kendar/i)])]");
        var jp = new String("$.[?(@.tags[?(@.requestedDomain=='www.kendar.com')])]");

        List<Map<String, Object>> result= JsonPath.compile(jp, new Predicate[]{}).read(toTest);
        assertEquals(2,result.size());
        var data = mapper.deserialize(mapper.serialize(result.get(0)), ReportDataEvent.class);
        assertEquals("dns-01",data.getInstanceId());
        data = mapper.deserialize(mapper.serialize(result.get(1)), ReportDataEvent.class);
        assertEquals("http-01",data.getInstanceId());
    }
}
