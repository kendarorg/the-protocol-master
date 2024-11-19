package org.kendar.mongo.plugins;

import org.kendar.plugins.MockPlugin;
import org.kendar.plugins.MockStorage;
import org.kendar.utils.ChangeableReference;

import java.util.List;

public class MongoMockPlugin extends MockPlugin<Object, Object> {
    @Override
    protected void checkMatching(MockStorage data, Object requestToSimulate, ChangeableReference<Integer> matchingQuery, ChangeableReference<Long> foundedIndex) {
        throw new RuntimeException("Not implemented");
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
