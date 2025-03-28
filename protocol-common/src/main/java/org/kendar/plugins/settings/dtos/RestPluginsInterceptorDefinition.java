package org.kendar.plugins.settings.dtos;

import org.kendar.plugins.base.ProtocolPhase;

public class RestPluginsInterceptorDefinition {
    private String inMatcher;
    private String outMatcher;
    private String inputType;
    private String outputType;
    private String destinationAddress;
    private ProtocolPhase phase;
    private boolean blockOnException = false;

    public String getOutMatcher() {
        return outMatcher;
    }

    public void setOutMatcher(String outMatcher) {
        this.outMatcher = outMatcher;
    }

    public boolean isBlockOnException() {
        return blockOnException;
    }

    public void setBlockOnException(boolean blockOnException) {
        this.blockOnException = blockOnException;
    }

    public ProtocolPhase getPhase() {
        return phase;
    }

    public void setPhase(ProtocolPhase phase) {
        this.phase = phase;
    }

    public String getInMatcher() {
        return inMatcher;
    }

    public void setInMatcher(String inMatcher) {
        this.inMatcher = inMatcher;
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
