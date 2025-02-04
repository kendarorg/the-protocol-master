package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpErrorPluginSettings extends PluginSettings {
    private int showError;
    private int errorPercent;
    private String errorMessage = "Error";
    private List<String> errorSites = new ArrayList<>();

    public List<String> getErrorSites() {
        return errorSites;
    }

    public void setErrorSites(List<String> errorSites) {
        this.errorSites = errorSites;
    }

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
