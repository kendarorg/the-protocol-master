package org.kendar.ui;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.di.annotations.TpmService;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.ui.dto.RecordingLine;
import org.kendar.ui.dto.RecordingSearchResult;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.parser.SimpleParser;
import org.kendar.utils.parser.Token;
import org.kendar.utils.parser.TokenType;

import java.util.Comparator;

import static org.kendar.apis.ApiUtils.respondKo;
import static org.kendar.plugins.GlobalReportPlugin.padLeftZeros;

@TpmService
@HttpTypeFilter(
        blocking = true)
public class RecordingHtmx implements FilteringClass {
    private final JsonMapper mapper;
    private final MultiTemplateEngine resolversFactory;
    private final StorageRepository storage;
    private final SimpleParser simpleParser;

    public RecordingHtmx(JsonMapper mapper, MultiTemplateEngine resolversFactory,
                         StorageRepository storage,
                         SimpleParser simpleParser) {
        this.mapper = mapper;
        this.resolversFactory = resolversFactory;
        this.storage = storage;
        this.simpleParser = simpleParser;
    }

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/recording",
            method = "GET", id = "GET /recording")
    public void recording(Request request, Response response) {
        resolversFactory.render("recording.jte", null, response);
    }

    @HttpMethodFilter(
            pathAddress = "/recording/search",
            method = "GET", id = "GET /recording/search")
    public void recordingSearch(Request request, Response response) {
        var model = buildData(request);
        resolversFactory.render("recording/index.jte", model, response);
    }

    @HttpMethodFilter(
            pathAddress = "/recording/search/{id}",
            method = "GET", id = "GET /recording/search/{id}")
    public void recordingGetFile(Request request, Response response) {
        var id = padLeftZeros(request.getPathParameter("id"), 10);
        var numericId = Long.parseLong(id);
        var index = storage.getAllIndexes(-1).stream().filter(i -> i.getIndex() == numericId).findFirst().orElse(null);
        if (index == null) {
            respondKo(response, "Index not found " + id, 404);
            return;
        }
        var model = new RecordingLine(index);
        var recordingFileItem = storage.readFile("scenario",
                id + "." + index.getProtocolInstanceId());
        StorageItem recordingItem = null;
        if (recordingFileItem != null) {
            recordingItem = mapper.deserialize(recordingFileItem, StorageItem.class);
        } else {
            recordingItem = new StorageItem();
            recordingItem.setIndex(index.getIndex());
            recordingItem.setCaller(index.getCaller());
            recordingItem.setType(index.getType());
            recordingItem.setDurationMs(0);
        }
        model.setData(recordingItem);
        resolversFactory.render("recording/single.jte", model, response);
    }

    private RecordingSearchResult buildData(Request reqp) {
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
        var indexes = storage.getAllIndexes(-1);
        indexes.sort(Comparator.comparingLong(CompactLine::getIndex));
        var lastTimestamp = 0L;
        for (var index : indexes) {
            var recordingLine = new RecordingLine(index);
            StorageItem recordingItem = null;
            var recordingFileItem = storage.readFile("scenario",
                    padLeftZeros(String.valueOf(index.getIndex()), 10) + "." + index.getProtocolInstanceId());
            if (recordingFileItem != null) {
                recordingItem = mapper.deserialize(recordingFileItem, StorageItem.class);
            }
            if (recordingItem == null) {
                recordingItem = new StorageItem();
                recordingItem.setIndex(index.getIndex());
                recordingItem.setCaller(index.getCaller());
                recordingItem.setType(index.getType());
                recordingItem.setDurationMs(0);
                recordingItem.setTimestamp(lastTimestamp);
                index.setTimestamp(lastTimestamp);
            } else {
                lastTimestamp = recordingItem.getTimestamp();
            }
            recordingLine.setData(recordingItem);
            var data = mapper.toJsonNode(recordingLine);
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
        var result = new RecordingSearchResult();
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
