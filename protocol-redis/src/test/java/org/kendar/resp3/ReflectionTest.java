package org.kendar.resp3;

import org.junit.jupiter.api.Test;
import org.kendar.redis.plugins.RedisRecordPlugin;
import org.kendar.storage.NullStorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;

import java.util.Arrays;
import java.util.regex.Pattern;

public class ReflectionTest {
    private Pattern pattern = Pattern.compile("(.*)\\((.*)\\)");

    @Test
    void testReflection() {
        var example = new RedisRecordPlugin(new JsonMapper(), new NullStorageRepository(),new MultiTemplateEngine());
        var clazz = example.getClass();
        var handle = Arrays.stream(clazz.getMethods()).filter(m ->
                m.getName().equalsIgnoreCase("handle")).findFirst();
        var matcher = pattern.matcher(handle.get().toString());
        if (matcher.find()) {
            var pars = matcher.group(2);
            System.out.println(pars);
        }
    }
}
