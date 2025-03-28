package org.kendar.plugins.settings.dtos;

import org.kendar.plugins.base.ProtocolPhase;

public class RestPluginsInterceptorDefinition {
    private String messageMatcher;
    private String inputType;
    private String outputType;
    private String destinationAddress;
    private ProtocolPhase phase;

    public ProtocolPhase getPhase() {
        return phase;
    }

    public void setPhase(ProtocolPhase phase) {
        this.phase = phase;
    }

    public String getMessageMatcher() {
        return messageMatcher;
    }

    public void setMessageMatcher(String messageMatcher) {
        this.messageMatcher = messageMatcher;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }
}
