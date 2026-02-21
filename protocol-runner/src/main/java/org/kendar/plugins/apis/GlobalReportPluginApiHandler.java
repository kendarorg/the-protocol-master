package org.kendar.plugins.apis;

import com.fasterxml.jackson.databind.node.BinaryNode;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.PathParameter;
import org.kendar.annotations.multi.QueryString;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.plugins.GlobalReport;
import org.kendar.plugins.GlobalReportPlugin;
import org.kendar.plugins.base.BasePluginApiHandler;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.MultiTemplateEngine;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;
import org.kendar.utils.parser.TokenType;

import java.util.List;

import static org.kendar.apis.ApiUtils.respondJson;
import static org.kendar.apis.ApiUtils.respondOk;

@SuppressWarnings("RegExpRedundantEscape")
@HttpTypeFilter()
public class GlobalReportPluginApiHandler implements BasePluginApiHandler {
    private static final JsonMapper mapper = new JsonMapper();
    private final GlobalReportPlugin plugin;
    private final StorageRepository repository;
    private final SimpleParser simpleParser;
    private final MultiTemplateEngine resolversFactory;

    public GlobalReportPluginApiHandler(GlobalReportPlugin plugin, StorageRepository repository, SimpleParser simpleParser,
                                        MultiTemplateEngine resolversFactory) {

        this.plugin = plugin;
        this.repository = repository;
        this.simpleParser = simpleParser;
        this.resolversFactory = resolversFactory;
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/{action}",
            method = "GET", id = "GET /api/global/plugins/report-plugin/{action}")
    @TpmDoc(
            description = "Handle the global report plugin actions start,stop,status,download",
            path = @PathParameter(key = "action",
                    allowedValues = {"start", "stop", "status", "download", "clean"}),
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ),
                    @TpmResponse(
                            body = Status.class,
                            description = "In case of status request"
                    ),
                    @TpmResponse(
                            body = GlobalReport.class,
                            description = "In case of download"
                    )
            },
            tags = {"plugins/global"})
    public boolean handleGlobalPluginActions(Request reqp, Response resp) {
        var action = reqp.getPathParameter("action");
        if ("download".equalsIgnoreCase(action)) {
            GlobalReport report = plugin.getReport();
            respondJson(resp, report);
            return true;
        } else if ("start".equalsIgnoreCase(action)) {
            plugin.setActive(true);
            respondOk(resp);
            return true;
        } else if ("stop".equalsIgnoreCase(action)) {
            plugin.setActive(false);
            respondOk(resp);
            return true;
        } else if ("clean".equalsIgnoreCase(action)) {
            plugin.clear();
            respondOk(resp);
            return true;
        } else if ("status".equalsIgnoreCase(action)) {
            var status = new Status();
            status.setActive(plugin.isActive());
            respondJson(resp, status);
            return true;
        }
        return false;
    }


    @Override
    public String getId() {
        return "global." + plugin.getId();
    }


    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/report",
            method = "DELETE", id = "DELETE /api/global/plugins/report-plugin/report")
    @TpmDoc(
            description = "Delete all report data",
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    )
            },
            tags = {"plugins/global"})
    public void deleteReports(Request reqp, Response response) {
        for (var file : repository.listFiles("global", "report-plugin")) {
            repository.deleteFile("global", "report-plugin", file);
        }
    }

    @HttpMethodFilter(
            pathAddress = "/api/global/plugins/report-plugin/report",
            method = "GET", id = "GET /api/global/plugins/report-plugin/report")
    @TpmDoc(
            description = "Handle the global report plugin data retrieval<br>Uses <a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> query language",
            query = {
                    @QueryString(key = "tpmql", description = "<a href='https://github.com/kendarorg/the-protocol-master/blob/main/docs/tpmql.md'>TPMql</a> selection query", example = ""),
                    @QueryString(key = "start", description = "Start from record"),
                    @QueryString(key = "limit", description = "Limit to n records"),
                    @QueryString(key = "format", description = "The return format, json|csv|html")
            },
            responses = {
                    @TpmResponse(
                            body = Ok.class
                    ),
                    @TpmResponse(
                            body = Object[].class,
                            description = "List of matching events"
                    )
            },
            tags = {"plugins/global"})
    public boolean loadReport(Request reqp, Response response) {
        var result = retrieveData(reqp);
        var format = reqp.getQuery("format");
        if (format == null) {
            format = "json";
        }

        if (format.equalsIgnoreCase("json")) {
            respondJson(response, result.getRows());
        } else if (format.equalsIgnoreCase("html")) {
            resolversFactory.render("global/report_plugin/html.jte", result, response);
        } else if (format.equalsIgnoreCase("csv")) {
            var sb = new StringBuilder();
            List<String> fields = result.getFields();
            sb.append("idx");
            for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                sb.append(",").append(fields.get(fieldIndex));
            }
            sb.append("\n");
            for (var rowIndex = 0; rowIndex < result.getRows().size(); rowIndex++) {
                var row = result.getRows().get(rowIndex);
                sb.append(rowIndex);
                for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                    var fieldValue = row.get(fields.get(fieldIndex));
                    sb.append(",");
                    if (fieldValue.isTextual()) {
                        sb.append("\"").append(fieldValue.asText().replaceAll("\\\"", "\\\\\"")).append("\"");
                    } else if (fieldValue.isObject()
                            || fieldValue.isArray()) {
                        sb.append("\"").append(fieldValue.toString().replaceAll("\\\"", "\\\\\"")).append("\"");
                    } else {
                        sb.append(result.convert(fieldValue));
                    }

                }
                sb.append("\n");
            }
            response.addHeader("Content-type", "text/csv");
            var binary = sb.toString().getBytes();
            response.setResponseText(new BinaryNode(binary));
            response.setStatusCode(200);
        }
        return true;
    }

    @HttpMethodFilter(
            pathAddress = "/global/plugins/report-plugin/report/search",
            method = "GET", id = "GET /global/plugins/report-plugin/report/search")
    public void retrieveHostsPage(Request reqp, Response response) {
        var model = retrieveData(reqp);
        resolversFactory.render("global/report_plugin/search.jte", model, response);
    }

    private GlobalReportResult retrieveData(Request reqp) {
        var tpmqlstring = reqp.getQuery("tpmql");
        var start = Integer.parseInt(reqp.getQuery("start"));
        var limit = Integer.parseInt(reqp.getQuery("limit"));
        var limitSet = limit > 0;
        Token tpmql = null;
        var isSelect = false;
        if (tpmqlstring != null && !tpmqlstring.isEmpty()) {
            tpmql = simpleParser.parse(tpmqlstring);
            isSelect = tpmql.value.equalsIgnoreCase("select") && tpmql.type == TokenType.FUNCTION;
        }

        var model = mapper.getMapper().createArrayNode();
        var allFiles = repository.listFiles("global", "report-plugin");
        for (var file : allFiles) {
            var text = repository.readFile("global", "report-plugin", file);
            var data = mapper.toJsonNode(text);

            if (!isSelect) {
                var isMatching = true;
                if (tpmql != null) {
                    isMatching = ((boolean) simpleParser.evaluate(tpmql, data));
                }
                if (isMatching) {
                    if (start > 0) {
                        start--;
                        continue;
                    }
                    if (limitSet) {
                        if (limit > 0) {
                            limit--;
                        } else {
                            break;
                        }
                    }
                    model.add(data);
                }
            } else {
                model.add(data);
            }
        }
        if (isSelect && tpmql != null) {
            var result = mapper.getMapper().createArrayNode();
            for (var item : simpleParser.select(tpmql, model)) {
                if (start > 0) {
                    start--;
                    continue;
                }
                if (limitSet) {
                    if (limit > 0) {
                        limit--;
                    } else {
                        break;
                    }
                }
                result.add(item);
            }
            model = result;
        }
        var result = new GlobalReportResult();
        result.setRows(model);
        if (!model.isEmpty()) {
            var fn = model.get(0).fieldNames();
            while (fn.hasNext()) {
                result.getFields().add(fn.next());
            }
        }
        return result;
    }
}
