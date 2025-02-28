package org.kendar.http.plugins.settings;

import org.kendar.plugins.settings.BasicPercentPluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpErrorPluginSettings extends BasicPercentPluginSettings {
    private int showError;
    private String errorMessage = "Error";
    private List<String> target = new ArrayList<>();

    public List<String> getTarget() {
        return target;
    }

    public void setTarget(List<String> target) {
        this.target = target;
    }

    public int getShowError() {
        return showError;
    }

    public void setShowError(int showError) {
        this.showError = showError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
