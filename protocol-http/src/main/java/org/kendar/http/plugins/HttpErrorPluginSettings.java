package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

public class HttpErrorPluginSettings extends PluginSettings {
    private int showError;
    private int errorPercent;
    private String errorMessage = "Error";

    public int getShowError() {
        return showError;
    }

    public void setShowError(int showError) {
        this.showError = showError;
    }

    public int getErrorPercent() {
        return errorPercent;
    }

    public void setErrorPercent(int errorPercent) {
        this.errorPercent = errorPercent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
