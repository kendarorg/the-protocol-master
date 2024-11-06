package org.kendar.http.plugins;

import org.kendar.filters.ProtocolPhase;
import org.kendar.filters.ProtocolPluginDescriptor;
import org.kendar.http.utils.Request;
import org.kendar.http.utils.Response;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RecordingPlugin extends ProtocolPluginDescriptor<Request, Response> {
    private static final Logger log = LoggerFactory.getLogger(RecordingPlugin.class);
    private final JsonMapper mapper = new JsonMapper();
    private final ConcurrentLinkedQueue<StorageItem<Request, Response>> items = new ConcurrentLinkedQueue<>();
    private final List<CompactLine> lines = new ArrayList<>();
    private final AtomicLong counter = new AtomicLong(0);
    private Path repository;
    private List<Pattern> recordSites = new ArrayList<>();

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    public void flush() {
        while (true) {
            try {
                if (items.isEmpty()) {
                    Sleeper.sleep(10);
                    continue;
                }
                var item = items.poll();
                if (item.getIndex() <= 0) {
                    var valueId = generateIndex();
                    item.setIndex(valueId);
                }
                var compactLine = new CompactLine();
                compactLine.setIndex(item.getIndex());
                compactLine.setCaller(item.getCaller());
                compactLine.setType(item.getType());
                compactLine.setDurationMs(item.getDurationMs());
                compactLine.getTags().put("path", item.getInput().getPath());
                compactLine.getTags().put("host", item.getInput().getHost());
                var query = String.join("&", item.getInput().getQuery().entrySet().stream().
                        sorted(Comparator.comparing(Map.Entry<String, String>::getKey)).
                        map(it -> it.getKey() + "=" + it.getValue()).collect(Collectors.toList()));

                compactLine.getTags().put("query", query);
                lines.add(compactLine);
                var id = padLeftZeros(String.valueOf(item.getIndex()), 10) + ".json";

                var result = mapper.serializePretty(item);
                Files.writeString(Path.of(repository.toString(), id), result);


            } catch (Exception e) {
                log.info("Error flushing ", e);
            }
        }
    }

    private long generateIndex() {
        return counter.incrementAndGet();
    }

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL);
    }

    @Override
    public String getId() {
        return "recording-plugin";
    }

    @Override
    public String getProtocol() {
        return "http";
    }


    @Override
    public void initialize(Map<String, Object> section) {
        var recordingPath = (String) section.get("datadir");
        setupSitesToRecord((String) section.get("recordSites"));
        recordingPath = recordingPath.replace("{milliseconds}", Calendar.getInstance().getTimeInMillis() + "");
        repository = Path.of(recordingPath);

        if (!Files.exists(repository)) {
            try {
                Files.createDirectories(repository);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        new Thread(this::flush).start();
    }

    private void setupSitesToRecord(String recordSites) {
        if (recordSites == null) recordSites = "";
        this.recordSites = List.of(recordSites.split(",")).stream()
                .map(s -> s.trim()).filter(s -> s.length() > 0)
                .map(s -> Pattern.compile(s)).collect(Collectors.toList());
    }

    @Override
    public boolean handle(ProtocolPhase phase, Request request, Response response) {
        if (recordSites.size() > 0) {
            var matchFound = false;
            for (var pat : recordSites) {
                if (pat.matcher(request.getHost()).matches()) {
                    matchFound = true;
                    break;
                }
            }
            if (!matchFound) {
                return false;
            }
        }
        var index = counter.incrementAndGet();
        var item = new StorageItem<Request, Response>();
        item.setIndex(index);
        item.setCaller("HTTP");
        item.setType(request.getMethod());
        var durationMs = Calendar.getInstance().getTimeInMillis() - request.getMs();
        item.setDurationMs(durationMs);
        item.setConnectionId(-1);
        item.setInput(request);
        item.setOutput(response);
        items.add(item);


        log.info("REC " + request.getMethod() + " " + request.buildUrl());
        return false;
    }

    @Override
    public void terminate() {
        var compact = mapper.serializePretty(lines);
        try {
            Files.writeString(Path.of(repository.toString(), "index.json"), compact);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
