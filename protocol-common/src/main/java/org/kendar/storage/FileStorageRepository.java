package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmPostConstruct;
import org.kendar.di.annotations.TpmService;
import org.kendar.events.*;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@TpmService(tags = "storage_file")
public class FileStorageRepository implements StorageRepository {
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
    private DiService diService;

    public FileStorageRepository(String targetDir) {

        this(Path.of(targetDir));
    }

    public FileStorageRepository(Path targetDir) {

        this.targetDir = targetDir.toAbsolutePath().toString();
    }

    @TpmConstructor
    public FileStorageRepository(GlobalSettings settings,DiService diService) {
        this.diService = diService;
        var logsDir = settings.getDataDir();
        if (logsDir == null || logsDir.isEmpty()) {
            logsDir = Path.of("data",
                    Long.toString(Calendar.getInstance().getTimeInMillis())).toAbsolutePath().toString();
        } else {
            logsDir = logsDir.replace("file=", "");
        }
        this.targetDir = Path.of(logsDir).toAbsolutePath().toString();
        targetDir = ensureDirectory(targetDir);
    }

    protected static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
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

    protected static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    @TpmPostConstruct
    @Override
    public void initialize() {

        try {
            targetDir = ensureDirectory(targetDir);
            EventsQueue.register("FileStorageRepository", (e) -> write(e.getLineToWrite()), WriteItemEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> finalizeWrite(e.getInstanceId()), FinalizeWriteEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> initializeContentWrite(e.getInstanceId()), StartWriteEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> finalizePlay(e.getInstanceId()), EndPlayEvent.class);
            EventsQueue.register("FileStorageRepository", (e) -> initializeContent(e.getInstanceId()), StartPlayEvent.class);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void finalizePlay(String instanceId) {
        synchronized (initializeContentLock) {
            log.info("Stop replaying {}", instanceId);
            protocolRepo.remove(instanceId);
        }
    }

    protected ProtocolRepo initializeContent(String instanceId) {

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

    protected void initializeContentWrite(String instanceId) {

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

    @Override
    public List<CompactLine> getIndexes(String instanceId) {
        if (protocolRepo.get(instanceId) == null) {
            return null;
        }
        var repo = protocolRepo.get(instanceId);
        return new ArrayList<>(repo.index);
    }

    @Override
    public void clean() {
        protocolRepo.clear();
        var dir = Path.of(targetDir).toFile();
        cleanRecursive(dir);
        EventsQueue.send(new StorageReloadedEvent());
    }

    public long generateIndex() {
        synchronized (lock) {
            return storageCounter.incrementAndGet();
        }
    }

    @Override
    public void write(LineToWrite item) {
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
                    if (!Files.exists(Paths.get(targetDir))) {
                        Files.createDirectories(Paths.get(targetDir));
                    }
                    writeContent(id, result);
                }
            } catch (Exception e) {
                log.error("[TPM ][WR]: Error writing item", e);
            } finally {
                executorItems.decrementAndGet();
            }
        });
    }

    private void writeContent(String id, String result) throws IOException {
        setFileContent(Path.of(targetDir, id), result);
    }

    protected List<CompactLine> retrieveIndexFile(String protocolInstanceId) {
        String fileContent;
        try {
            fileContent = getFileContent(Path.of(targetDir, "index." + protocolInstanceId + ".json"));
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
                var fileContent = getFileContent(Path.of(targetDir, fileName));
                result.add(mapper.deserialize(fileContent, typeReference));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void finalizeWrite(String instanceId) {
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
            if (Files.exists(Path.of(targetDir, indexFile))) {
                Files.delete(Path.of(targetDir, indexFile));
            }
            setFileContent(Path.of(targetDir, indexFile),
                    mapper.serializePretty(repo.index));

        } catch (IOException e) {
            log.error("[TPM  ][WR]: Unable to write index file");
            throw new RuntimeException(e);
        }

        log.info("Stop recording {}", instanceId);
    }

    @Override
    public StorageItem readById(String protocolInstanceId, long id) {
        var ctx = protocolRepo.get(protocolInstanceId);
        if (ctx == null) {
            String fileContent;
            try {
                var filePath = Path.of(targetDir, padLeftZeros(String.valueOf(id), 10) + "." + protocolInstanceId + ".json");

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

    @Override
    public List<StorageItem> readResponses(String protocolInstanceId, ResponseItemQuery query) {
        var ctx = protocolRepo.get(protocolInstanceId);
        var result = new ArrayList<StorageItem>();

        log.debug("[CL<FF] loading responses {}", query.getStartAt());
        for (var item : ctx.index.stream()
                .sorted(Comparator.comparingInt(value -> (int) value.getIndex())).
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
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .collect(Collectors.toList());
    }

    @Override
    public byte[] readAsZip() {
        var baos = new ByteArrayOutputStream();
        var globalSettings = diService.getInstance(GlobalSettings.class);
        var globalSettingsFile = mapper.serialize(globalSettings);
        try {
            Files.writeString(Path.of(targetDir,"settings.json").toAbsolutePath(),globalSettingsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (var zos = new ZipOutputStream(baos)) {
            for (var file : new File(Path.of(targetDir).toAbsolutePath().toString()).listFiles()) {
                zipFile(file, file.getName(), zos);
            }
        } catch (IOException ioe) {
            log.error("Error Creating storage zip", ioe);
        }
        return baos.toByteArray();
    }

    @Override
    public void writeZip(byte[] byteArray) {
        String settingsDir= null;
        var destDirString = Path.of(targetDir).toAbsolutePath().toString();
        File destDir = new File(destDirString);
        // create output directory if it doesn't exist
        if (!destDir.exists()) destDir.mkdirs();
        ByteArrayInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new ByteArrayInputStream(byteArray);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry == null) {
                throw new RuntimeException("Not a zip file!");
            }
            while (zipEntry != null) {
                if(zipEntry.getName().equalsIgnoreCase("settings.json") &&
                        Path.of(targetDir).toAbsolutePath().compareTo(destDir.toPath().toAbsolutePath()) == 0) {
                    settingsDir =Path.of(destDir.getAbsolutePath(),zipEntry.getName()).toString();
                }
                File newFile = newFile(destDir, zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
            EventsQueue.send(new StorageReloadedEvent().withSettings(settingsDir));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getType() {
        return "storage";
    }

    @Override
    public List<CompactLineComplete> getAllIndexes(int maxLen) {
        var result = new ArrayList<CompactLineComplete>();
        for (var file : listFilesUsingJavaIO(Path.of(targetDir).toAbsolutePath().toString())) {
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


                    var filePath = Path.of(targetDir, padLeftZeros(String.valueOf(item.getIndex()), 10) + "." + protocolInstanceId + ".json");
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

    @Override
    public void update(long itemId, String protocolInstanceId, CompactLine index, StorageItem item) {
        var indexFile = retrieveIndexFile(protocolInstanceId);
        var indexPath = Path.of(targetDir, "index." + protocolInstanceId + ".json");
        for (int i = 0; i < indexFile.size(); i++) {
            var toCheck = indexFile.get(i);
            if (toCheck.getIndex() == itemId) {
                index.setIndex(itemId);
                indexFile.set(i, index);
                var old = this.readById(protocolInstanceId, itemId);

                var filePath = Path.of(targetDir, padLeftZeros(String.valueOf(itemId), 10) + "." + protocolInstanceId + ".json");
                try {
                    item.setIndex(itemId);
                    item.setTimestamp(old.getTimestamp());
                    item.setCaller(old.getCaller());
                    setFileContent(indexPath, mapper.serialize(indexFile));
                    setFileContent(filePath, mapper.serialize(item));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
        }
        throw new RuntimeException("ITem id " + itemId + " not found");

    }

    @Override
    public void delete(String protocolInstanceId, long itemId) {
        try {
            var indexFile = retrieveIndexFile(protocolInstanceId);
            var filePath = Path.of(targetDir, padLeftZeros(String.valueOf(itemId), 10) + "." + protocolInstanceId + ".json");
            var indexPath = Path.of(targetDir, "index." + protocolInstanceId + ".json");
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
            throw new RuntimeException("Unable to delete item " + itemId, ex);
        }
    }

    @Override
    public List<StorageFileIndex> listPluginFiles(String instanceId, String pluginId) {
        var result = new ArrayList<StorageFileIndex>();
        var pluginDir = ensureDirectory(Path.of(targetDir, instanceId, pluginId).toAbsolutePath().toString());
        for (var file : listFilesUsingJavaIO(Path.of(pluginDir).toAbsolutePath().toString())) {
            if (file.getName().endsWith(".json")) {
                result.add(new StorageFileIndex(instanceId, pluginId, file.getName().replace(".json", "")));
            }
        }
        return result;
    }

    @Override
    public StorageFile readPluginFile(StorageFileIndex index) {
        var pluginDir = ensureDirectory(Path.of(targetDir, index.getInstanceId(), index.getPluginId()).toAbsolutePath().toString());
        var filePath = Path.of(pluginDir, index.getIndex() + ".json");
        if (Files.exists(filePath)) {
            try {
                return new StorageFile(index, getFileContent(filePath));
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    @Override
    public void writePluginFile(StorageFile file) {
        var pluginDir = ensureDirectory(Path.of(targetDir, file.getIndex().getInstanceId(), file.getIndex().getPluginId()).toAbsolutePath().toString());
        var filePath = Path.of(pluginDir, file.getIndex().getIndex() + ".json");
        try {
            setFileContent(filePath, file.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void delPluginFile(StorageFileIndex file) {
        var pluginDir = ensureDirectory(Path.of(targetDir, file.getInstanceId(), file.getPluginId()).toAbsolutePath().toString());
        var filePath = Path.of(pluginDir, file.getIndex() + ".json");
        if (filePath.toFile().exists()) {
            filePath.toFile().delete();
        }
    }

    protected String getFileContent(Path of) throws IOException {
        return Files.readString(of);
    }

    protected void setFileContent(Path of, String s) throws IOException {
        Files.writeString(of, s);
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
