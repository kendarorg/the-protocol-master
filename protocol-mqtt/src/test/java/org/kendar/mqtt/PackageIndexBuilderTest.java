package org.kendar.mqtt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PackageIndexBuilderTest {
    @Test
    void test01() {
        var usedPackets = new ArrayList<Integer>();
        usedPackets.add(1);
        usedPackets.add(3);
        usedPackets.add(2);
        var list = usedPackets.stream().sorted(Integer::compare).collect(Collectors.toList());
        var maxFoundedIndex = 64000;
        for (var i = list.size() - 1; i >= 0; i--) {
            var index = list.get(i);
            if (index > 45000) {
                maxFoundedIndex = index;
            } else {
                break;
            }
        }
        usedPackets.add(maxFoundedIndex - 1);
        System.out.println(list);
    }
}
