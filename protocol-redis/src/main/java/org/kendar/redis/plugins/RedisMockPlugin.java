package org.kendar.redis.plugins;

import org.kendar.plugins.MockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.utils.ChangeableReference;

import java.util.List;

public class RedisMockPlugin extends MockPlugin<Object, Object> {
    @Override
    protected void checkMatching(MockStorage data, Object requestToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {

    }

    @Override
    protected void writeOutput(Object request, Object response, MockStorage founded) {

    }

    @Override
    protected List<MockStorage> firstCheckOnMainPart(Object request) {
        return List.of();
    }

    @Override
    public String getProtocol() {
        return "mongodb";
    }
}
