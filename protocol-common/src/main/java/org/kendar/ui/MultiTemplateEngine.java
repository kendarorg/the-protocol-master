package org.kendar.ui;

import com.fasterxml.jackson.databind.node.TextNode;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateException;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.TPMPluginsClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;

@TpmService
public class MultiTemplateEngine {

    private JsonMapper mapper;
    private MultiCodeResolver resolver;
    private TemplateEngine templateEngine;

    public MultiTemplateEngine() {

    }

    @TpmConstructor
    public MultiTemplateEngine(MultiCodeResolver resolver, TPMPluginsClassLoader TPMPluginsClassLoader) {
        this.mapper = new JsonMapper();
        this.resolver = resolver;
        this.templateEngine = TemplateEngine.create(this.resolver,
                Paths.get("jte-classes"),
                ContentType.Html, TPMPluginsClassLoader);
    }

    private static void respondException(Response response, Exception e) {
        var sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String result = "<html><head><title>Error rendering template</title></head><body>" +
                "<h1>Error rendering template</h1>" +
                "<p>" +
                e.getMessage() +
                "</p>" +
                "<pre>" +
                sw +
                "</pre>" +
                "</body></html>" +
                sw;
        response.addHeader("Content-type", "text/html");
        response.setResponseText(new TextNode(result));
        response.setStatusCode(500);
    }

    public void render(String name, Object param, TemplateOutput output) throws TemplateException {
        if (templateEngine == null) {
            return;
        }

        try {
            templateEngine.render(name, param, output);
        }catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage());
        }

    }
    private static final Logger log = LoggerFactory.getLogger(MultiTemplateEngine.class);

    private HashSet<String> stringTemplates = new HashSet<>();

    public void render(String name, Object model, Response response) throws TemplateException {
        if (templateEngine == null) {
            return;
        }
        try {
            var output = new StringOutput();

                    templateEngine.render(name, model, output);


            response.addHeader("Content-type", "text/html");
            response.setResponseText(new TextNode(output.toString()));
            response.setStatusCode(200);
        } catch (Exception e) {
            log.error("Error rendering {}",name, e);
            respondException(response, e);
        }
    }

    public void render(String name, Map<String, Object> params, TemplateOutput output) throws TemplateException {
        if (templateEngine == null) {
            return;
        }
        templateEngine.render(name, params, output);
    }

    public void render(String name, Map<String, Object> model, Response response) throws TemplateException {
        if (templateEngine == null) {
            return;
        }
        try {
            var output = new StringOutput();
            templateEngine.render(name, model, output);
            response.addHeader("Content-type", "text/html");
            response.setResponseText(new TextNode(output.toString()));
            response.setStatusCode(200);
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
