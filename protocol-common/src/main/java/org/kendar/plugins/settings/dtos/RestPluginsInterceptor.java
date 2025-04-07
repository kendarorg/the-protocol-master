package org.kendar.plugins.settings.dtos;

import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;

import java.util.regex.Pattern;

public class RestPluginsInterceptor {
    private static final SimpleParser parser = new SimpleParser();
    private static final JsonMapper mapper = new JsonMapper();
    private String inMatcher;
    private String outMatcher;
    private Token inToken;
    private Token outToken;
    private String inputType = "Object";
    private String outputType = "Object";
    private String destinationAddress;
    private ProtocolPhase phase;
    private boolean blockOnException = false;
    private Pattern outPattern = null;
    private Pattern inPattern = null;
    private boolean initialized = false;
    private boolean specialInMatch = false;
    private boolean specialOutMatch = false;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutMatcher() {
        return outMatcher;
    }

    public void setOutMatcher(String outMatcher) {
        this.outMatcher = outMatcher;
    }

    private void initialize() {
        if (initialized) {
            return;
        }
        if (getInMatcher() != null) {
            if (getInMatcher().startsWith("@")) {
                specialInMatch = true;
                this.inPattern = Pattern.compile(getInMatcher().substring(1));
            } else if (getInMatcher().startsWith("!")) {
                specialInMatch = true;
                this.inToken = parser.parse(getInMatcher().substring(1));
            } else {
                this.inMatcher = getInMatcher();
            }
        }
        if (getOutMatcher() != null) {
            if (getOutMatcher().startsWith("@")) {
                specialOutMatch = true;
                this.outPattern = Pattern.compile(getOutMatcher().substring(1));
            } else if (getOutMatcher().startsWith("!")) {
                specialOutMatch = true;
                this.outToken = parser.parse(getOutMatcher().substring(1));
            } else {
                this.outMatcher = getOutMatcher();
            }
        }
        initialized = true;
    }

    public boolean matches(String in, String out) {
        initialize();
        if (inPattern != null && !inPattern.matcher(in).matches()) return false;
        if (outPattern != null && !outPattern.matcher(out).matches()) return false;
        if (inToken != null) {
            if (in == null || !(boolean) parser.evaluate(inToken, mapper.toJsonNode(in))) return false;
        }
        if (outToken != null) {
            if (out == null || !(boolean) parser.evaluate(outToken, mapper.toJsonNode(out))) return false;
        }
        if (inMatcher != null && !inMatcher.isEmpty() && !specialInMatch) {
            if (in == null || !in.contains(inMatcher)) return false;
        }

        if (outMatcher != null && !outMatcher.isEmpty() && !specialOutMatch) {
            if (out == null || !out.contains(outMatcher)) return false;
        }
        return true;
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
