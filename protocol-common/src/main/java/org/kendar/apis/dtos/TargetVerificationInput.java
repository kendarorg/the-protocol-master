package org.kendar.apis.dtos;

public class TargetVerificationInput {
    private String target;
    private String matchAgainst;

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMatchAgainst() {
        return matchAgainst;
    }

    public void setMatchAgainst(String matchAgainst) {
        this.matchAgainst = matchAgainst;
    }
}
