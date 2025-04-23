package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmPostConstruct;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.*;
import org.kendar.exceptions.TPMException;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ResultOfMethodCallIgnored")
@TpmService(tags = "storage_file")
public class FileStorageRepository extends StorageRepository {
    protected static final JsonMapper mapper = new JsonMapper();
    protected static final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected static final Logger log = LoggerFactory.getLogger(FileStorageRepository.class);
    protected final ConcurrentHashMap<String, ProtocolRepo> protocolRepo = new ConcurrentHashMap<>();
    protected final AtomicInteger storageCounter = new AtomicInteger(0);
    protected final TypeReference<StorageItem> typeReference = new TypeReference<>() {
    };
    protected final Object initializeContentLock = new Object();
    protected final AtomicInteger executorItems = new AtomicInteger(0);
    protected final Object lock = new Object();
    protected String targetDir;
    protected DiService diService;

    public FileStorageRepository(String targetDir) {

        this(Path.of(targetDir));
    }

    public FileStorageRepository(Path targetDir) {
        super(null, null);
        this.targetDir = targetDir.toAbsolutePath().toString();
    }

    protected FileStorageRepository() {
        super(null, null);

    }

    @TpmConstructor
    public FileStorageRepository(GlobalSettings settings, DiService diService) {
        super(diService, mapper);
        initializeStorageRepo(settings, diService);

    }

    /**
     * Pad the names of the recordings to allow easy ordering
     *
     * @param inputString
     * @param length
     * @return
     */
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

    /**
     * Certify that a directory exists
     *
     * @param td
     * @return
     */
    protected static String ensureDirectory(String td) {
        if (!Path.of(td).isAbsolute()) {
            Path currentRelativePath = Paths.get("").toAbsolutePath();
            td = Path.of(currentRelativePath.toString(), td).toString();
        }
        if (!Files.exists(Path.of(td))) {
            if (!Path.of(td).toFile().mkdirs()) {
                log.error("Error creating target dir {}", td);
            }
        }
        return td;
    }

    /**
     * Delete -everything-
     *
     * @param dir
     */
    protected static void cleanRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    cleanRecursive(file);
                }
                file.delete();
            }
        }
    }

    private String getScenarioDir() {
        return Path.of(targetDir, "scenario").toString();
    }

    protected void initializeStorageRepo(GlobalSettings settings, DiService diService) {
        this.diService = diService;
        var dataDir = settings.getDataDir();
        if (dataDir == null || dataDir.isEmpty()) {
            dataDir = Path.of("data",
                    Long.toString(Calendar.getInstance().getTimeInMillis())).toAbsolutePath().toString();
        } else {
            dataDir = dataDir.replace("file=", "");
        }
        this.targetDir = Path.of(dataDir).toAbsolutePath().toString();
        targetDir = ensureDirectory(targetDir);
        ensureDirectory(getScenarioDir());
    }

    @TpmPostConstruct

    public void initialize() {

        try {
            targetDir = ensureDirectory(targetDir);
            ensureDirectory(getScenarioDir());
            EventsQueue.register("FileStorageRepository", (e) -> recordInteraction(e.getLineToWrite()), WriteItemEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> finalizeRecording(e.getInstanceId()), FinalizeWriteEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> initializeContentForWrite(e.getInstanceId()), StartWriteEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> finalizePlay(e.getInstanceId()), EndPlayEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> initializeContentForReplay(e.getInstanceId()), StartPlayEvent.class);

        } catch (Exception e) {
            throw new TPMException(e);
        }
    }


    /**
     * Conclude the replaying for a given protocol
     *
     * @param instanceId
     */
    protected void finalizePlay(String instanceId) {
        synchronized (initializeContentLock) {
            log.info("Stop replaying {}", instanceId);
            protocolRepo.remove(instanceId);
        }
    }

    /**
     * Initiazlie the writing reading for a specific protocol
     *
     * @param instanceId
     * @return
     */
    protected ProtocolRepo initializeContentForReplay(String instanceId) {

        return protocolRepo.compute(instanceId, (protocolInstanceId, currRepo) -> {
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
                currRepo.index.sort(Comparator.comparing(CompactLine::getTimestamp));
                if (!currRepo.index.isEmpty()) {
                    var maxRepoIndex = currRepo.index.stream().max(Comparator.comparing(CompactLine::getIndex));
                    var maxIndex = Math.max(storageCounter.get(), maxRepoIndex.get().getIndex() + 1);
                    storageCounter.set((int) maxIndex);
                }
                currRepo.initialized = true;
                log.info("Start replaying {}", instanceId);
            }
            return currRepo;
        });
    }

    protected void initializeContentForWrite(String instanceId) {
        protocolRepo.compute(instanceId, (protocolInstanceId, currRepo) -> {
            if (currRepo == null) {
                currRepo = new ProtocolRepo();
            }
            if (!currRepo.initialized) {
                currRepo.index = retrieveIndexFile(protocolInstanceId);
                if (!currRepo.index.isEmpty()) {
                    var maxRepoIndex = currRepo.index.stream().max(Comparator.comparing(CompactLine::getIndex));
                    var maxIndex = Math.max(storageCounter.get(), maxRepoIndex.get().getIndex() + 1);
                    storageCounter.set((int) maxIndex);
                }
                currRepo.initialized = true;
                log.info("Start recording {}", instanceId);
            }
            return currRepo;
        });
    }

    /**
     * Retrieve indexes for recording/replaying
     *
     * @param instanceId
     * @return
     */

    public List<CompactLine> getIndexes(String instanceId) {
        if (protocolRepo.get(instanceId) == null) {
            return null;
        }
        var repo = protocolRepo.get(instanceId);
        return new ArrayList<>(repo.index);
    }

    /**
     * Reset all storage
     */

    public void clean() {
        protocolRepo.clear();
        var dir = Path.of(targetDir).toFile();
        cleanRecursive(dir);
        EventsQueue.send(new StorageReloadedEvent());
    }

    @Override
    public boolean existsFile(String... path) {
        var realPath = buildRealPath(path) + ".json";
        var fullPath = Path.of(realPath);
        var parent = fullPath.getParent().toFile();
        if (!parent.exists()) {
            return false;
        }
        return fullPath.toFile().exists();
    }

    /**
     * Create unique progressive id
     *
     * @return
     */
    public long generateIndex() {
        synchronized (lock) {
            return storageCounter.incrementAndGet();
        }
    }

    /**
     * Record a line
     *
     * @param item
     */

    public void recordInteraction(LineToWrite item) {
        executorItems.incrementAndGet();
        executor.submit(() -> {

            if (item == null) {
                log.error("No line to send founded");
                return;
            }
            try {
                var valueId = item.getId();
                item.getCompactLine().setIndex(valueId);
                if (item.getStorageItem() != null) {
                    item.getStorageItem().setIndex(valueId);
                }

                var id = padLeftZeros(String.valueOf(valueId), 10) + "." + item.getInstanceId() + ".json";

                var repo = protocolRepo.get(item.getInstanceId());
                repo.index.add(item.getCompactLine());
                repo.somethingWritten = true;
                if (item.getStorageItem() != null) {
                    var result = mapper.serializePretty(item.getStorageItem());
                    ensureDirectory(getScenarioDir());
                    setFileContent(Path.of(targetDir, "scenario", id), result);
                }
            } catch (Exception e) {
                log.error("[TPM ][WR]: Error writing item", e);
            } finally {
                executorItems.decrementAndGet();
            }
        });
    }


    /**
     * Retrieve all the index data for recording
     *
     * @param protocolInstanceId
     * @return
     */
    protected List<CompactLine> retrieveIndexFile(String protocolInstanceId) {
        String fileContent;
        try {
            fileContent = getFileContent(Path.of(targetDir, "scenario", "index." + protocolInstanceId + ".json"));
        } catch (IOException e) {
            fileContent = "[]";
        }
        return mapper.deserialize(fileContent, new TypeReference<>() {
        });
    }

    /**
     * Retrieve all scenario data
     *
     * @param protocolInstanceId
     * @return
     */
    protected List<StorageItem> readAllItems(String protocolInstanceId) {
        ensureDirectory(getScenarioDir());
        var fileNames = Stream.of(new File(getScenarioDir()).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(name -> name.endsWith("." + protocolInstanceId + ".json"))
                .sorted()
                .toList();
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
                var fileContent = getFileContent(Path.of(targetDir, "scenario", fileName));
                result.add(mapper.deserialize(fileContent, typeReference));
            } catch (IOException e) {
                throw new TPMException(e);
            }
        }
        return result;
    }

    /**
     * Finalize recording
     *
     * @param instanceId
     */

    public void finalizeRecording(String instanceId) {
        try {
            Sleeper.sleepNoException(1000, () -> executorItems.get() == 0, true);
            var repo = protocolRepo.get(instanceId);
            if (repo == null) return;
            protocolRepo.remove(instanceId);
            if (!repo.somethingWritten) {
                protocolRepo.remove(instanceId);
                return;
            }
            var indexFile = "index." + instanceId + ".json";
            if (Files.exists(Path.of(targetDir, "scenario", indexFile))) {
                Files.delete(Path.of(targetDir, "scenario", indexFile));
            }
            repo.index.sort(Comparator.comparing(CompactLine::getTimestamp));
            setFileContent(Path.of(targetDir, "scenario", indexFile),
                    mapper.serializePretty(repo.index));

        } catch (IOException e) {
            log.error("[TPM  ][WR]: Unable to write index file");
            throw new TPMException(e);
        }

        log.info("Stop recording {}", instanceId);
    }

    /**
     * Read item from scenario
     *
     * @param protocolInstanceId
     * @param id
     * @return
     */

    public StorageItem readFromScenarioById(String protocolInstanceId, long id) {
        var ctx = protocolRepo.get(protocolInstanceId);
        if (ctx == null) {
            String fileContent;
            try {
                var filePath = Path.of(targetDir, "scenario", padLeftZeros(String.valueOf(id), 10) + "." + protocolInstanceId + ".json");

                fileContent = getFileContent(filePath);
            } catch (IOException e) {
                fileContent = "{}";
            }
            return mapper.deserialize(fileContent, new TypeReference<>() {
            });
        }
        var result = ctx.inMemoryDb.get(id);
        if (result == null) {
            result = ctx.outItems.stream().filter(a -> a.getIndex() == id).findFirst().orElse(null);
        }
        return result;
    }

    /**
     * Read all responses for scenario given data
     *
     * @param protocolInstanceId
     * @param query
     * @return
     */

    public List<StorageItem> readResponsesFromScenario(String protocolInstanceId, ResponseItemQuery query) {
        var ctx = protocolRepo.get(protocolInstanceId);
        var result = new ArrayList<StorageItem>();

        log.debug("[CL<FF] loading responses {}", query.getStartAt());
        for (var item : ctx.index.stream()
                .sorted(Comparator.comparingLong(CompactLine::getIndex)).
                filter(value -> value.getIndex() > query.getStartAt()).
                toList()) {
            if (query.getUsed().contains((int) item.getIndex())) continue;
            if (item.getType().equalsIgnoreCase("RESPONSE")) {
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

    protected List<File> listFilesUsingJavaIO(String dir) {
        var files = new File(dir).listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        return Stream.of(files)
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }

    protected List<File> listDirsUsingJavaIO(String dir) {
        var files = new File(dir).listFiles();
        if (files == null) {
            return new ArrayList<>();
        }
        return Stream.of(files)
                .filter(File::isDirectory)
                .collect(Collectors.toList());
    }


    public String getType() {
        return "storage";
    }

    /**
     * Retrieve all index data
     *
     * @param maxLen
     * @return
     */

    public List<CompactLineComplete> getAllIndexes(int maxLen) {
        var result = new ArrayList<CompactLineComplete>();
        for (var file : listFilesUsingJavaIO(Path.of(targetDir, "scenario").toAbsolutePath().toString())) {
            if (file.getName().contains("index") && file.getName().endsWith(".json")) {
                String fileContent;
                var fileNameOnly = file.toPath().getFileName().toString();
                fileNameOnly = fileNameOnly.replace("index.", "");
                var protocolInstanceId = fileNameOnly.replace(".json", "");
                try {
                    fileContent = getFileContent(file.toPath());
                } catch (IOException e) {
                    fileContent = "{}";
                }
                var deserialized = mapper.deserialize(fileContent, new TypeReference<List<CompactLineComplete>>() {
                });
                deserialized.forEach(item -> {
                    item.setProtocolInstanceId(protocolInstanceId);
                    var id = protocolInstanceId + "/" + padLeftZeros(String.valueOf(item.getIndex()), 10);


                    var filePath = Path.of(targetDir, "scenario", padLeftZeros(String.valueOf(item.getIndex()), 10) + "." + protocolInstanceId + ".json");
                    if (filePath.toFile().exists()) {
                        item.setFullItemId(id);
                    }
                    if (maxLen >= 0) {
                        for (var tag : item.getTags().entrySet()) {
                            var val = tag.getValue();
                            var isTooMuch = val.length() > maxLen;
                            val = val.substring(0, Math.min(val.length(), maxLen));
                            if (isTooMuch) {
                                val += "...";
                            }
                            item.getTags().put(tag.getKey(), val);
                        }
                    }
                });
                result.addAll(deserialized);
            }
        }
        return result;

    }


    public void updateRecording(long itemId, String protocolInstanceId, CompactLine index, StorageItem item) {
        var indexFile = retrieveIndexFile(protocolInstanceId);
        var indexPath = Path.of(targetDir, "scenario", "index." + protocolInstanceId + ".json");
        for (int i = 0; i < indexFile.size(); i++) {
            var toCheck = indexFile.get(i);
            if (toCheck.getIndex() == itemId) {
                index.setIndex(itemId);
                indexFile.set(i, index);
                var old = this.readFromScenarioById(protocolInstanceId, itemId);

                var filePath = Path.of(targetDir, "scenario", padLeftZeros(String.valueOf(itemId), 10) + "." + protocolInstanceId + ".json");
                try {
                    item.setIndex(itemId);
                    item.setTimestamp(old.getTimestamp());
                    item.setCaller(old.getCaller());
                    setFileContent(indexPath, mapper.serialize(indexFile));
                    setFileContent(filePath, mapper.serialize(item));
                } catch (IOException e) {
                    throw new TPMException(e);
                }
                return;
            }
        }
        throw new TPMException("ITem id " + itemId + " not found");

    }


    public void deleteRecording(String protocolInstanceId, long itemId) {
        try {
            var indexFile = retrieveIndexFile(protocolInstanceId);
            var filePath = Path.of(targetDir, "scenario", padLeftZeros(String.valueOf(itemId), 10) + "." + protocolInstanceId + ".json");
            var indexPath = Path.of(targetDir, "scenario", "index." + protocolInstanceId + ".json");
            if (filePath.toFile().exists()) {
                Files.delete(filePath);
            }
            for (int i = 0; i < indexFile.size(); i++) {
                var toCheck = indexFile.get(i);
                if (toCheck.getIndex() == itemId) {
                    indexFile.remove(i);
                    setFileContent(indexPath, mapper.serialize(indexFile));
                    break;
                }
            }
        } catch (Exception ex) {
            throw new TPMException("Unable to delete item " + itemId, ex);
        }
    }


    public StorageFile readPluginFile(StorageFileIndex file) {
        var realPath = buildRealPath(file.getInstanceId(), file.getPluginId()).toString();
        ensureDirectory(realPath);
        var filePath = Path.of(realPath, file.getIndex() + ".json");
        if (Files.exists(filePath)) {
            try {
                return new StorageFile(file, getFileContent(filePath));
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }


    public void writePluginFile(StorageFile file) {
        var realPath = buildRealPath(file.getIndex().getInstanceId(), file.getIndex().getPluginId()).toString();
        ensureDirectory(realPath);
        var filePath = Path.of(realPath, file.getIndex().getIndex() + ".json");
        try {
            setFileContent(filePath, file.getContent());
        } catch (IOException e) {
            throw new TPMException(e);
        }

    }

    protected String getFileContent(Path of) throws IOException {
        return Files.readString(of);
    }

    protected void setFileContent(Path of, String s) throws IOException {
        Files.writeString(of, s);
    }

    public List<String> listFiles(String... path) {
        var realPath = buildRealPath(path);
        return listFilesUsingJavaIO(realPath.toString()).stream().map(s -> s.getName().replace(".json", "")
        ).sorted().collect(Collectors.toList());
    }

    public List<String> listDirs(String... path) {
        var realPath = buildRealPath(path);
        return listDirsUsingJavaIO(realPath.toString()).stream().map(File::getName).sorted().collect(Collectors.toList());
    }

    public boolean fileExists(String... path) {
        var realPath = buildRealPath(path);
        if (Files.isDirectory(realPath)) {
            Files.exists(realPath);
        }
        return Files.exists(Path.of(realPath + ".json"));
    }

    public void writeFile(String content, String... path) {
        var realPath = buildRealPath(path) + ".json";
        var fullPath = Path.of(realPath);
        var parent = fullPath.getParent().toFile();
        if (!parent.exists()) {
            fullPath.getParent().toFile().mkdirs();
        }

        try {
            setFileContent(fullPath, content);
        } catch (IOException e) {
            throw new TPMException(e);
        }
    }

    @Override
    public void deleteFile(String... path) {
        var realPath = buildRealPath(path) + ".json";
        var rp = Path.of(realPath);
        try {
            if (!Files.exists(rp)) {
                return;
            }
            Files.deleteIfExists(rp);
        } catch (IOException e) {
            throw new TPMException(e);
        }
    }

    public String readFile(String... path) {
        var realPath = buildRealPath(path) + ".json";
        var rp = Path.of(realPath);
        try {
            if (!Files.exists(rp)) {
                return null;
            }
            return getFileContent(rp);
        } catch (IOException e) {
            throw new TPMException(e);
        }
    }

    private Path buildRealPath(String... path) {
        var fullPath = new ArrayList<>(Arrays.asList(path));
        var realPath = Path.of(targetDir);
        if (!fullPath.isEmpty()) {
            realPath = Path.of(targetDir, fullPath.toArray(new String[0]));
        }
        var root = Path.of(targetDir);
        if (!realPath.toAbsolutePath().toString().contains(root.toAbsolutePath().toString())) {
            throw new TPMException("Cannot naviagate outside project!");
        }
        return realPath;
    }

    protected static class ProtocolRepo {
        public final Object lockObject = new Object();
        public final ConcurrentHashMap<Long, StorageItem> inMemoryDb = new ConcurrentHashMap<>();
        public final List<StorageItem> outItems = new ArrayList<>();
        public List<CompactLine> index = new ArrayList<>();
        public boolean initialized = false;
        public volatile boolean somethingWritten = false;
    }
}
