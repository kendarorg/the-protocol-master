package org.kendar.apis.dto;

public class JdbcForwardDto {
    private String id;
    private String source;
    private String target;
    public JdbcForwardDto(String id, String source, String target){
        this.id = id;

        this.source = source;
        this.target = target;
    }
    public JdbcForwardDto(){

    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
