package org.kendar.di.simple.tpls;

import org.kendar.di.annotations.TpmService;

@TpmService
public class UserOfStringTpl implements GenericUserOf {
    private final TemplateInterface<String> template;

    public UserOfStringTpl(TemplateInterface<String> template) {

        this.template = template;
        System.out.println(this.toString() + " " + Thread.currentThread().getId());
    }

    @Override
    public String toString() {
        return "UserOfStringTpl{" +
                "template=" + template +
                '}';
    }

    public TemplateInterface<String> getTemplate() {
        return template;
    }
}
