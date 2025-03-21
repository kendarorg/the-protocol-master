package org.kendar.plugins.apis.dtos;

public class RecordItemFile {
    private String body;
    private String fileName;
    private String instanceId;

    public RecordItemFile(String instanceId, String body, String fileName) {
        this.instanceId = instanceId;
        this.body = body;
        this.fileName = fileName;
    }

    public RecordItemFile() {
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
