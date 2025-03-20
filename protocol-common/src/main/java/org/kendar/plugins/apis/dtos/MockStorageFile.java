package org.kendar.plugins.apis.dtos;

import org.kendar.plugins.MockStorage;

public class MockStorageFile {
    private MockStorage mockStorage;
    private String fileName;
    private String protocolInstanceId;

    public MockStorageFile() {

    }

    public MockStorageFile(String protocolInstanceId, String fileName, MockStorage mockStorage) {
        this.protocolInstanceId = protocolInstanceId;
        this.mockStorage = mockStorage;
        this.fileName = fileName;
    }

    public String getProtocolInstanceId() {
        return protocolInstanceId;
    }

    public void setProtocolInstanceId(String protocolInstanceId) {
        this.protocolInstanceId = protocolInstanceId;
    }

    public MockStorage getMockStorage() {
        return mockStorage;
    }

    public void setMockStorage(MockStorage mockStorage) {
        this.mockStorage = mockStorage;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
