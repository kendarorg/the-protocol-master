package org.kendar.plugins.dtos;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.settings.dtos.RestPluginsInterceptorDefinition;

import java.util.regex.Pattern;

public class RestPluginsInterceptor {
    private Pattern outPattern = null;
    private Pattern inPattern = null;
    private String outMatcher = null;
    private ProtocolPhase phase;
    private String inputType;
    private String inMatcher = null;

    public boolean isBlockOnException() {
        return blockOnException;
    }

    private boolean blockOnException;

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
        this.blockOnException = interceptorDefinition.isBlockOnException();
        if(interceptorDefinition.getInMatcher()!=null){
            if(interceptorDefinition.getInMatcher().startsWith("@")){
                this.inPattern = Pattern.compile(interceptorDefinition.getInMatcher().substring(1));
            }else{
                this.inMatcher=interceptorDefinition.getInMatcher();
            }
        }
        if(interceptorDefinition.getOutMatcher()!=null){
            if(interceptorDefinition.getOutMatcher().startsWith("@")){
                this.outPattern = Pattern.compile(interceptorDefinition.getInMatcher().substring(1));
            }else{
                this.outMatcher=interceptorDefinition.getOutMatcher();
            }
        }
    }

    public boolean matches(String in,String out){
        if(inMatcher!=null && !inMatcher.isEmpty()){
            if(in==null || !in.contains(inMatcher))return false;
        }
        if(outMatcher!=null && !outMatcher.isEmpty()){
            if(out==null || !out.contains(outMatcher))return false;
        }
        if(inPattern!=null){
            if(in==null || !inPattern.matcher(in).matches())return false;
        }
        if(outPattern!=null ){
            if(out==null || !outPattern.matcher(out).matches())return false;
        }
        return true;
    }
}
