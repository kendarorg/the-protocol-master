package org.kendar.ui;

import gg.jte.output.StringOutput;
import org.kendar.di.DiService;

public class OptionalTemplate {

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
        }catch (Exception e){
            return "";
        }
    }
}
