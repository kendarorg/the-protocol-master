package org.kendar.ui.dto;

public class WildcarPluginDto  extends BaseHtmxDto {
    private int active;
    private int notActive;
    private String id;

    public WildcarPluginDto() {
    }

    public WildcarPluginDto(String id) {

        this.id = id;
    }

    public boolean someActive() {
        return active > 0 && notActive > 0;
    }

    public int getActive() {
        return active;
    }

    public void setActive(int active) {
        this.active = active;
    }

    public int getNotActive() {
        return notActive;
    }

    public void setNotActive(int notActive) {
        this.notActive = notActive;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
