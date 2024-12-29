package org.kendar.di.simple.tpls;

import org.kendar.annotations.TpmService;

@TpmService
public class UserOfStringTpl implements GenericUserOf{
    private final TemplateInterface<String> template;

    public TemplateInterface<String> getTemplate() {
        return template;
    }

    public UserOfStringTpl(TemplateInterface<String> template) {

        this.template = template;
    }
}
