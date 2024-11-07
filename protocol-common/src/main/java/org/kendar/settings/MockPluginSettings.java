package org.kendar.settings;

public class MockPluginSettings extends PluginSettings{
    private boolean mock;

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }
}
