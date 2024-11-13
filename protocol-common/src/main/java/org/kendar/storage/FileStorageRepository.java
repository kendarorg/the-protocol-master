package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.storage.generic.*;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileStorageRepository implements StorageRepository {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(FileStorageRepository.class);
    private final ConcurrentLinkedQueue<LineToWrite> items = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, ProtocolRepo> protocolRepo = new ConcurrentHashMap<>();
    private final AtomicInteger storageCounter = new AtomicInteger(0);
    private final TypeReference<StorageItem> typeReference = new TypeReference<>() {
    };
    private String targetDir;

    public FileStorageRepository(String targetDir) {

        this.targetDir = targetDir;
    }

    public FileStorageRepository(Path targetDir) {

        this.targetDir = targetDir.toAbsolutePath().toString();
    }

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

    @Override
    public void initialize() {

        try {
            if (!Path.of(targetDir).isAbsolute()) {
                Path currentRelativePath = Paths.get("").toAbsolutePath();
                targetDir = Path.of(currentRelativePath.toString(), targetDir).toString();
            }
            if (!Files.exists(Path.of(targetDir))) {
                if (!Path.of(targetDir).toFile().mkdirs()) {
                    log.error("Error creating target dir {}", targetDir);
                }
            }
            new Thread(this::flush).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ProtocolRepo initializeContent(String protocolInstanceIdOuter) {
        return protocolRepo.compute(protocolInstanceIdOuter, (protocolInstanceId, currRepo) -> {
            if (currRepo == null) {
                currRepo = new ProtocolRepo();
            }
            if (!currRepo.initialized) {
                for (var item : readAllItems(protocolInstanceId)) {
                    if (item == null) continue;
                    if (item.getType() == null) continue;
                    if (item.getType().equalsIgnoreCase("RESPONSE")) {
                        currRepo.outItems.add(item);
                        continue;
                    }
                    currRepo.inMemoryDb.put(item.getIndex(), item);
                }

                currRepo.index = retrieveIndexFile(protocolInstanceId);
                if (!currRepo.index.isEmpty()) {
                    var maxRepoIndex = currRepo.index.stream().max(Comparator.comparing(CompactLine::getIndex));
                    var maxIndex = Math.max(storageCounter.get(), maxRepoIndex.get().getIndex() + 1);
                    storageCounter.set((int) maxIndex);
                }
                currRepo.initialized = true;
            }
            return currRepo;
        });
    }

    public long generateIndex() {
        return storageCounter.incrementAndGet();
    }

    @Override
    public void flush() {

        while (true) {
            try {
                if (items.isEmpty()) {
                    Sleeper.sleep(10);
                    continue;
                }
                var item = items.poll();
                //if (item.getIndex() <= 0) {
                var valueId = generateIndex();
                item.getCompactLine().setIndex(valueId);
                if (item.getStorageItem() != null) {
                    item.getStorageItem().setIndex(valueId);
                }
                initializeContent(item.getInstanceId());
                //}
                var id = padLeftZeros(String.valueOf(valueId), 10) + "." + item.getInstanceId() + ".json";

                var repo = protocolRepo.get(item.getInstanceId());
                repo.index.add(item.getCompactLine());
                if (item.getStorageItem() != null) {
                    var result = mapper.serializePretty(item.getStorageItem());
                    if (!Files.exists(Paths.get(targetDir))) {
                        Files.createDirectories(Paths.get(targetDir));
                    }
                    Files.writeString(Path.of(targetDir, id), result);
                }
            } catch (Exception e) {
                log.warn("Trouble flushing " + e);
            }
        }
    }

    @Override
    public void write(LineToWrite item) {
        if (item == null) {
            log.error("Blank item");
        } else {
            items.add(item);
        }
    }


    protected List<CompactLine> retrieveIndexFile(String protocolInstanceId) {
        String fileContent;
        try {
            fileContent = Files.readString(Path.of(targetDir, "index." + protocolInstanceId + ".json"));
        } catch (IOException e) {
            fileContent = "[]";
        }
        return mapper.deserialize(fileContent, new TypeReference<>() {
        });
    }

    protected List<StorageItem> readAllItems(String protocolInstanceId) {
        if (!Files.exists(Paths.get(targetDir))) {
            return new ArrayList<>();
        }
        var fileNames = Stream.of(new File(targetDir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(name -> name.endsWith("." + protocolInstanceId + ".json"))
                .sorted()
                .collect(Collectors.toList());
        var result = new ArrayList<StorageItem>();
        for (var fileName : fileNames) {
            var nameOnly = fileName.replace("." + protocolInstanceId + ".json", "");
            if (nameOnly.equalsIgnoreCase("index")) continue;
            try {
                Long.parseLong(nameOnly);
            } catch (NumberFormatException ex) {
                continue;
            }
            try {
                var fileContent = Files.readString(Path.of(targetDir, fileName));
                result.add(mapper.deserialize(fileContent, typeReference));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void finalizeWrite(String protocolInstanceId) {
        try {
            var repo = protocolRepo.get(protocolInstanceId);
            if (repo == null) return;
            var indexFile = "index." + protocolInstanceId + ".json";
            if (Files.exists(Path.of(targetDir, indexFile))) {
                Files.delete(Path.of(targetDir, indexFile));
            }
            Files.writeString(Path.of(targetDir, indexFile),
                    mapper.serializePretty(repo.index));
            protocolRepo.remove(protocolInstanceId);
        } catch (IOException e) {
            log.error("[TPM  ][WR]: Unable to write index file");
            throw new RuntimeException(e);
        }

        log.debug("[TPM  ][WR]: Optimized recording");
    }

    @Override
    public LineToRead read(String protocolInstanceId, CallItemsQuery query) {
        var ctx = initializeContent(protocolInstanceId);
        synchronized (ctx.lockObject) {

            var idx = ctx.index.stream()
                    .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                    .filter(a ->
                            typeMatching(query.getType(), a.getType()) &&
                                    a.getCaller().equalsIgnoreCase(query.getCaller()) &&
                                    tagsMatching(a.getTags(), query) &&
                                    query.getUsed().stream().noneMatch((n) -> n == a.getIndex())
                    ).findFirst();

            Optional<StorageItem> item = Optional.empty();

            if (idx.isPresent()) {
                item = ctx.inMemoryDb.values().stream()
                        .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                        .filter(a -> a.getIndex() == idx.get().getIndex()).findFirst();
            } else {
                log.warn("[TPM  ][WR]: Index not found!");
            }

            if (item.isPresent()) {

                log.debug("[SERVER][REPFULL]  {}:{}", item.get().getIndex(), item.get().getType());
                ctx.inMemoryDb.remove(item.get().getIndex());
                idx.ifPresent(compactLine -> ctx.index.remove(compactLine));
                return new LineToRead(item.get(), idx.get());
            }

            if (idx.isPresent()) {
                log.debug("[SERVER][REPSHRT] {}:{}", idx.get().getIndex(), idx.get().getType());
                ctx.index.remove(idx.get());
                var si = new StorageItem();
                si.setIndex(idx.get().getIndex());

                return new LineToRead(si, idx.get());
            }

            return null;
        }
    }

    private boolean typeMatching(String type, String type1) {
        if ("RESPONSE".equalsIgnoreCase(type1)) return false;
        if (type == null || type.isEmpty()) return true;
        return type.equalsIgnoreCase(type1);
    }

    private boolean tagsMatching(Map<String, String> tags, CallItemsQuery query) {
        for (var tag : query.getTags().entrySet()) {
            if (tags.containsKey(tag.getKey())) {
                var l = tags.get(tag.getKey());
                var r = query.getTags().get(tag.getKey());
                if ((l == null || r == null) && l == r) {
                    continue;
                }
                if (l.equalsIgnoreCase(r)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public List<StorageItem> readResponses(String protocolInstanceId, ResponseItemQuery query) {
        var ctx = initializeContent(protocolInstanceId);
        var result = new ArrayList<StorageItem>();
        for (var item : ctx.index.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex())).filter(a -> a.getIndex() > query.getStartAt()).collect(Collectors.toList())) {
            if (item.getType().equalsIgnoreCase("RESPONSE")) {
                log.debug("[CL<FF] loading response");
                var outItem = ctx.outItems.stream().filter(a -> a.getIndex() == item.getIndex()).findFirst();
                if (outItem.isPresent()) {
                    result.add(outItem.get());
                    log.debug("[CL<FF][CB] After: {} Index: {} Type: {}", query.getStartAt(), item.getIndex(), outItem.get().getType());
                }
            } else {
                break;
            }
        }
        return result;
    }

    @Override
    public String getType() {
        return "storage";
    }

    private class ProtocolRepo {
        public final Object lockObject = new Object();
        public final ConcurrentHashMap<Long, StorageItem> inMemoryDb = new ConcurrentHashMap<>();
        public final List<StorageItem> outItems = new ArrayList<>();
        public List<CompactLine> index = new ArrayList<>();
        public boolean initialized = false;
    }
}
