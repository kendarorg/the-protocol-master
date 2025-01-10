package org.kendar.tpl;

import com.fasterxml.jackson.databind.node.TextNode;
import io.marioslab.basis.template.Template;
import io.marioslab.basis.template.TemplateContext;
import io.marioslab.basis.template.TemplateLoader;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Response;
import org.kendar.utils.JsonMapper;

import java.util.concurrent.ConcurrentHashMap;


public abstract class BaseTemplate implements FilteringClass {
    protected static final JsonMapper mapper = new JsonMapper();
    protected static ConcurrentHashMap<String, Template> templates = new ConcurrentHashMap<>();

    protected Template loadTemplate(String filename) {
        filename = filename.toLowerCase();
        if (!templates.containsKey(filename)) {
            var loader = new TemplateLoader.ClasspathTemplateLoader();
            var template = loader.load(filename);
            templates.put(filename, template);
        }
        return templates.get(filename);
    }

    protected TemplateContext getTemplateContext() {
        return new TemplateContext();
    }

    protected void render(TemplateContext context, Template template, Response response) {
        var result = template.render(context);
        response.setResponseText(new TextNode(result));
    }
}
