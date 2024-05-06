package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic file storage implementations
 *
 * @param <I> input type
 * @param <O> output type
 */
public abstract class BaseFileStorage<I, O> extends BaseStorage<I, O> {


    protected static final JsonMapper mapper = new JsonMapper();
    private static final Logger log = LoggerFactory.getLogger(BaseFileStorage.class);
    protected String targetDir;

    public BaseFileStorage(String targetDir) {

        this.targetDir = targetDir;
    }

    public BaseFileStorage(Path targetDir) {

        this.targetDir = targetDir.toString();
    }

    protected List<CompactLine> retrieveIndexFile() {
        String fileContent = null;
        try {
            fileContent = Files.readString(Path.of(targetDir, "index.json"));
        } catch (IOException e) {
            log.error("Missing index file!");
            throw new RuntimeException(e);
        }
        return mapper.deserialize(fileContent, new TypeReference<List<CompactLine>>() {
        });
    }

    /**
     * Initialize (prepare the directory)
     */
    @Override
    public void initialize() {
        try {
            if (!Path.of(targetDir).isAbsolute()) {
                Path currentRelativePath = Paths.get("").toAbsolutePath();
                targetDir = Path.of(currentRelativePath.toString(), targetDir).toString();
            }
            if (!Files.exists(Path.of(targetDir))) {
                Path.of(targetDir).toFile().mkdirs();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read all items in the specific directory and add to list
     *
     * @return
     */
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
                result.add((StorageItem<I, O>) mapper.deserialize(fileContent, getTypeReference()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * Build the types to be serialized
     *
     * @return
     */
    protected abstract TypeReference<?> getTypeReference();

    /**
     * Write a storage item line
     *
     * @param item
     */
    protected void write(StorageItem item) {
        try {
            var valueId = ProtoDescriptor.getCounter("STORAGE_ID");
            var id = BaseStorage.padLeftZeros(String.valueOf(valueId), 10) + ".json";
            item.setIndex(valueId);
            var result = mapper.serializePretty(item);
            Files.writeString(Path.of(targetDir, id), result);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void optimize() {

        List<CompactLine> compactLines = new ArrayList<>();

        List<StorageItem<I, O>> loadedData = new ArrayList<>();
        try {
            for (var item : readAllItems()) {
                var cl = new CompactLine(item, () -> buildTag(item));
                compactLines.add(cl);
                if (!useFullData && shouldNotSave(cl, compactLines, item, loadedData)) {
                    var id = BaseStorage.padLeftZeros(String.valueOf(cl.getIndex()), 10) + ".json";
                    if (Files.exists(Path.of(targetDir, id + ".noop"))) {
                        Files.delete(Path.of(targetDir, id + ".noop"));
                    }
                    Files.move(Path.of(targetDir, id), Path.of(targetDir, id + ".noop"));
                    continue;
                }
                loadedData.add(item);
            }
            if (Files.exists(Path.of(targetDir, "index.json"))) {
                Files.delete(Path.of(targetDir, "index.json"));
            }
            Files.writeString(Path.of(targetDir, "index.json"), mapper.serializePretty(compactLines));
        } catch (IOException e) {
            log.error("[SERVER] Unable to write index file");
            throw new RuntimeException(e);
        }

        log.debug("[SERVER] Optimized recording");
    }

    protected abstract boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<I, O> item, List<StorageItem<I, O>> loadedData);

    protected abstract Map<String, String> buildTag(StorageItem<I, O> item);


}
