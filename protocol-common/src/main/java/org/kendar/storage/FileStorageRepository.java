package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileStorageRepository<I, O> implements StorageRepository<I, O> {
    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(FileStorageRepository.class);
    private static final Object lockObject = new Object();
    private final ConcurrentHashMap<Long, StorageItem<I, O>> inMemoryDb = new ConcurrentHashMap<>();
    private final List<StorageItem<I, O>> outItems = new ArrayList<>();
    private final Object responseLockObject = new Object();
    private final ConcurrentLinkedQueue<StorageItem> items = new ConcurrentLinkedQueue<>();
    private String targetDir;
    private BaseStorage<I, O> baseStorage;
    private List<CompactLine> index = new ArrayList<>();
    private boolean initialized = false;
    private ProtoDescriptor descriptor;

    public FileStorageRepository(String targetDir) {

        this.targetDir = targetDir;
    }

    public FileStorageRepository(Path targetDir) {

        this.targetDir = targetDir.toString();
    }

    @Override
    public void initialize(BaseStorage<I, O> baseStorage) {
        this.baseStorage = baseStorage;
        this.descriptor = baseStorage.getDescriptor();
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

    private void initializeContent() {
        if (!initialized) {
            for (var item : readAllItems()) {
                if (item.getType().equalsIgnoreCase("RESPONSE")) {
                    outItems.add(item);
                    continue;
                }
                inMemoryDb.put(item.getIndex(), item);
            }

            index = retrieveIndexFile();
            initialized = true;
        }
    }

    public long generateIndex() {
        return descriptor.getCounter("STORAGE_ID");
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
                if (item.getIndex() <= 0) {
                    var valueId = generateIndex();
                    item.setIndex(valueId);
                }
                var id = BaseStorage.padLeftZeros(String.valueOf(item.getIndex()), 10) + ".json";

                var result = mapper.serializePretty(item);
                Files.writeString(Path.of(targetDir, id), result);

            } catch (Exception e) {
                log.warn("Trouble flushing "+e);
            }
        }
    }

    @Override
    public void write(StorageItem item) {
        items.add(item);
    }


    protected List<CompactLine> retrieveIndexFile() {
        String fileContent;
        try {
            fileContent = Files.readString(Path.of(targetDir, "index.json"));
        } catch (IOException e) {
            log.error("Missing index file!");
            throw new RuntimeException(e);
        }
        return mapper.deserialize(fileContent, new TypeReference<>() {
        });
    }

    protected List<StorageItem<I, O>> readAllItems() {
        var fileNames = Stream.of(new File(targetDir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
        var result = new ArrayList<StorageItem<I, O>>();
        for (var fileName : fileNames) {
            var nameOnly = fileName.replace(".json", "");
            try {
                Long.parseLong(nameOnly);
            } catch (NumberFormatException ex) {
                continue;
            }
            try {
                var fileContent = Files.readString(Path.of(targetDir, fileName));
                result.add((StorageItem<I, O>) mapper.deserialize(fileContent, baseStorage.getTypeReference()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void optimize() {


        List<CompactLine> compactLines = new ArrayList<>();

        List<StorageItem<I, O>> loadedData = new ArrayList<>();
        try {
            for (var item : readAllItems()) {
                var cl = new CompactLine(item, () -> baseStorage.buildTag(item));
                compactLines.add(cl);
                if (!baseStorage.useFullData() && baseStorage.shouldNotSave(cl, compactLines, item, loadedData)) {
                    var id = BaseStorage.padLeftZeros(String.valueOf(cl.getIndex()), 10) + ".json";
                    if (Files.exists(Path.of(targetDir, id + ".noop"))) {
                        Files.delete(Path.of(targetDir, id + ".noop"));
                    }
                    try {
                        Files.move(Path.of(targetDir, id), Path.of(targetDir, id + ".noop"));
                    } catch (NoSuchFileException ex) {
                        log.warn("[TPM  ][WR]: File did not exist at {}", Path.of(targetDir, id));
                    }
                    continue;
                }
                loadedData.add(item);
            }
            if (Files.exists(Path.of(targetDir, "index.json"))) {
                Files.delete(Path.of(targetDir, "index.json"));
            }
            Files.writeString(Path.of(targetDir, "index.json"), mapper.serializePretty(compactLines));
        } catch (IOException e) {
            log.error("[TPM  ][WR]: Unable to write index file");
            throw new RuntimeException(e);
        }

        log.debug("[TPM  ][WR]: Optimized recording");
    }

    @Override
    public StorageItem read(CallItemsQuery query) {
        synchronized (lockObject) {
            initializeContent();

            var idx = index.stream()
                    .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                    .filter(a ->
                            typeMatching(query.getType(), a.getType()) &&
                                    a.getCaller().equalsIgnoreCase(query.getCaller()) &&
                                    tagsMatching(a.getTags(), query) &&
                                    query.getUsed().stream().noneMatch((n) -> n == a.getIndex())
                    ).findFirst();

            Optional<StorageItem<I, O>> item = Optional.empty();

            CompactLine cl = null;
            if (idx.isPresent()) {
                cl = idx.get();
                item = inMemoryDb.values().stream()
                        .sorted(Comparator.comparingInt(value -> (int) value.getIndex()))
                        .filter(a -> a.getIndex() == idx.get().getIndex()).findFirst();
            } else {
                log.warn("[TPM  ][WR]: Index not found!");
            }
            var shouldNotSave = baseStorage.shouldNotSave(cl, null, null, null);

            if (item.isPresent() && !shouldNotSave) {

                log.debug("[SERVER][REPFULL]  {}:{}", item.get().getIndex(), item.get().getType());
                inMemoryDb.remove(item.get().getIndex());
                idx.ifPresent(compactLine -> index.remove(compactLine));
                return baseStorage.beforeSendingReadResult(item.get(), null);
            }

            if (idx.isPresent()) {
                log.debug("[SERVER][REPSHRT] {}:{}", idx.get().getIndex(), idx.get().getType());
                index.remove(idx.get());
                var si = new StorageItem<I, O>();
                si.setIndex(idx.get().getIndex());

                return baseStorage.beforeSendingReadResult(si, idx.get());
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
    public List<StorageItem> readResponses(ResponseItemQuery query) {
        var result = new ArrayList<StorageItem>();
        for (var item : index.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex())).filter(a -> a.getIndex() > query.getStartAt()).collect(Collectors.toList())) {
            if (item.getType().equalsIgnoreCase("RESPONSE")) {
                log.debug("[CL<FF] loading response");
                var outItem = outItems.stream().filter(a -> a.getIndex() == item.getIndex()).findFirst();
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
}
