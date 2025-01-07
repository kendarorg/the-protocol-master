package org.kendar.di.simple.tpls;

import org.kendar.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;

import java.util.List;

@TpmService
public class UserOfIntTpl implements GenericUserOf {
    private List<TemplateInterface<Integer>> template;

    public List<TemplateInterface<Integer>> getTemplate() {
        return template;
    }

    public UserOfIntTpl(){

    }

    @TpmConstructor
    public UserOfIntTpl(List<TemplateInterface<Integer>> template) {

        this.template = template;
    }
}
