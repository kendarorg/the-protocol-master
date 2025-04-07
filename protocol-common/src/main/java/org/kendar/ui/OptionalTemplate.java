package org.kendar.ui;

import gg.jte.TemplateNotFoundException;
import gg.jte.output.StringOutput;
import org.kendar.di.DiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionalTemplate {
    private static final Logger log = LoggerFactory.getLogger(OptionalTemplate.class);
    private final Object data;
    private final String template;
    private final String genericTemplate;

    public OptionalTemplate(Object data, String template, String genericTemplate) {
        this.data = data;
        this.template = template.replaceAll("-", "_");
        if (genericTemplate != null) {
            this.genericTemplate = genericTemplate.replaceAll("-", "_");
        } else {
            this.genericTemplate = null;
        }

    }

    public String getTemplate() {
        var usedTemplate = template;
        try {
            var mte = DiService.getThreadContext().getInstance(MultiTemplateEngine.class);
            var output = new StringOutput();
            try {
                mte.render(template, data, output);

                return output.toString();
            } catch (TemplateNotFoundException e) {
                if (genericTemplate != null) {
                    usedTemplate = genericTemplate;
                    mte.render(genericTemplate, data, output);
                    return output.toString();
                }
                log.debug("Not found template for {}", template);
                return "";
            }
        } catch (TemplateNotFoundException e) {
            log.debug("Not found template for {} or {}", template, genericTemplate);
            return "";
        } catch (Exception e) {
            log.error("Error while generating template for {}", usedTemplate, e);
            return "";
        }
    }
}
