package org.kendar.plugins.dtos;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.dtos.RestPluginsInterceptor;

public class RestPluginCall {
    private String inputType;
    private String ouputType;
    private ProtocolPhase phase;
    private String input;
    private String output;

    public RestPluginCall() {
    }

    public RestPluginCall(RestPluginsInterceptor interceptor, String input, String output) {
        this.input = input;
        this.output = output;
        this.inputType = interceptor.getInputType();
        this.ouputType = interceptor.getOutputType();
        this.phase = interceptor.getPhase();
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOuputType() {
        return ouputType;
    }

    public void setOuputType(String ouputType) {
        this.ouputType = ouputType;
    }

    public ProtocolPhase getPhase() {
        return phase;
    }

    public void setPhase(ProtocolPhase phase) {
        this.phase = phase;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
