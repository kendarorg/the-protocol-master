package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;

import java.util.List;

@TpmService
public class UserOfIntTpl implements GenericUserOf {
    private List<TemplateInterface<Integer>> template;

    public UserOfIntTpl() {

    }

    @Override
    public String toString() {
        return "UserOfIntTpl{" +
                "template=" + template +
                '}';
    }

    @TpmConstructor
    public UserOfIntTpl(List<TemplateInterface<Integer>> template) {

        this.template = template;
        System.out.println(this.toString()+" "+Thread.currentThread().getId());
    }

    public List<TemplateInterface<Integer>> getTemplate() {
        return template;
    }
}
