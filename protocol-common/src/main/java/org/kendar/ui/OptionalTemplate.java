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

    public OptionalTemplate(Object data, String template) {
        this.data = data;
        this.template = template.replaceAll("-","_");
    }

    public String getTemplate() {
        try {
            var mte = DiService.getThreadContext().getInstance(MultiTemplateEngine.class);
            var output = new StringOutput();
            mte.render(template, data, output);
            return output.toString();
        }catch (TemplateNotFoundException e){
            log.info("Not found template for {}", template);
            return "";
        }catch (Exception e){
            log.error("Error while generating template for {}", template, e);
            return "";
        }
    }
}
