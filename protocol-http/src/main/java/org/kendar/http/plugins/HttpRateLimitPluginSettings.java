package org.kendar.http.plugins;

import org.kendar.settings.PluginSettings;

import java.util.ArrayList;
import java.util.List;

public class HttpRateLimitPluginSettings extends PluginSettings {
    private String headerLimit = "RateLimit-Limit";
    private String headerRemaining = "RateLimit-Remaining";
    private String headerReset = "RateLimit-Reset";
    private String headerRetryAfter = "Retry-After";
    private int costPerRequest = 2;
    private int resetTimeWindowSeconds = 60;
    private int warningThresholdPercent = 80;
    private int rateLimit = 120;//or custom?
    private String resetFormat = "secondsLeft";//UtcEpochSeconds
    private String customResponseFile;
    private List<String> limitSites = new ArrayList<>();

    public List<String> getLimitSites() {
        return limitSites;
    }

    public void setLimitSites(List<String> limitSites) {
        this.limitSites = limitSites;
    }

    public String getHeaderLimit() {
        return headerLimit;
    }

    public void setHeaderLimit(String headerLimit) {
        this.headerLimit = headerLimit;
    }

    public String getHeaderRemaining() {
        return headerRemaining;
    }

    public void setHeaderRemaining(String headerRemaining) {
        this.headerRemaining = headerRemaining;
    }

    public String getHeaderReset() {
        return headerReset;
    }

    public void setHeaderReset(String headerReset) {
        this.headerReset = headerReset;
    }

    public String getHeaderRetryAfter() {
        return headerRetryAfter;
    }

    public void setHeaderRetryAfter(String headerRetryAfter) {
        this.headerRetryAfter = headerRetryAfter;
    }

    public int getCostPerRequest() {
        return costPerRequest;
    }

    public void setCostPerRequest(int costPerRequest) {
        this.costPerRequest = costPerRequest;
    }

    public int getResetTimeWindowSeconds() {
        return resetTimeWindowSeconds;
    }

    public void setResetTimeWindowSeconds(int resetTimeWindowSeconds) {
        this.resetTimeWindowSeconds = resetTimeWindowSeconds;
    }

    public int getWarningThresholdPercent() {
        return warningThresholdPercent;
    }

    public void setWarningThresholdPercent(int warningThresholdPercent) {
        this.warningThresholdPercent = warningThresholdPercent;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(int rateLimit) {
        this.rateLimit = rateLimit;
    }


    public String getResetFormat() {
        return resetFormat;
    }

    public void setResetFormat(String resetFormat) {
        this.resetFormat = resetFormat;
    }

    public String getCustomResponseFile() {
        return customResponseFile;
    }

    public void setCustomResponseFile(String customResponseFile) {
        this.customResponseFile = customResponseFile;
    }
}
