package org.kendar.plugins.settings.dtos;

import java.util.regex.Pattern;

public class ForwardMatcher {
    private String id;
    private String oriSource;
    private String oriTarget;
    private Pattern source;
    private String target;

    //source: jdbc:mysql://([a-ZA-Z0-9_\\-]+)/([a-ZA-Z0-9_\\-]+)[\\?]{0-1}([.]*)
    //source: jdbc:mysql://([a-ZA-Z0-9_\\.\\-]+):3306/volagratis[\\?]{0-1}([.]*)
    //dest  : jdbc:mysql://rds-volagratis/volagratis?$2&blah=true
    public ForwardMatcher(String source, String target) {
        this.id= org.kendar.utils.Md5Tester.calculateMd5(source+target);
        this.oriSource =source;
        this.oriTarget = target;
        this.source = Pattern.compile(source);
        this.target = target;
    }

    public String match(String rec) {
        var matcher = source.matcher(rec);
        if(matcher.matches()){
            return matcher.replaceAll(target);
        }
        return null;
    }

    public String getOriSource() {
        return oriSource;
    }

    public void setOriSource(String oriSource) {
        this.oriSource = oriSource;
    }

    public String getOriTarget() {
        return oriTarget;
    }

    public void setOriTarget(String oriTarget) {
        this.oriTarget = oriTarget;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
