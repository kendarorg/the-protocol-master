package org.kendar.plugins.dtos;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.dtos.RestPluginsInterceptorDefinition;

public class RestPluginsInterceptor {
    private final ProtocolPhase phase;
    private final String inputType;

    public String getOutputType() {
        return outputType;
    }

    public String getInputType() {
        return inputType;
    }

    public ProtocolPhase getPhase() {
        return phase;
    }

    private final String outputType;

    public String getTarget() {
        return target;
    }

    private final String target;

    public RestPluginsInterceptor(RestPluginsInterceptorDefinition interceptorDefinition) {
        this.target = interceptorDefinition.getDestinationAddress();
        this.phase = interceptorDefinition.getPhase();
        this.inputType = interceptorDefinition.getInputType();
        this.outputType = interceptorDefinition.getOutputType();
    }
}
