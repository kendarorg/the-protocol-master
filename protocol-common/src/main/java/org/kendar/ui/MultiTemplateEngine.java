package org.kendar.ui;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import org.kendar.di.annotations.TpmService;

import java.util.Map;

@TpmService
public class MultiTemplateEngine {

    private final MultiCodeResolver resolver;
    private final TemplateEngine templateEngine;

    public MultiTemplateEngine(MultiCodeResolver resolver) {
        this.resolver = resolver;
        this.templateEngine = TemplateEngine.create(this.resolver, ContentType.Html);
    }

    public void render(String name, Object param, TemplateOutput output) throws TemplateException {
        templateEngine.render(name, param, output);
    }

    public void render(String name, Map<String, Object> params, TemplateOutput output) throws TemplateException {
        templateEngine.render(name, params, output);
    }
}
