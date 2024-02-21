package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.utils.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseFileStorage<I, O> extends BaseStorage<I, O> {
    protected static final AtomicLong counter = new AtomicLong(0);

    protected static final JsonMapper mapper = new JsonMapper();


    protected String targetDir;

    public BaseFileStorage(String targetDir) {

        this.targetDir = targetDir;
    }

    public BaseFileStorage(Path targetDir) {

        this.targetDir = targetDir.toString();
    }

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

    protected List<StorageItem<I, O>> readAllItems() {
        var fileNames = Stream.of(new File(targetDir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
        var result = new ArrayList<StorageItem<I, O>>();
        for (var fileName : fileNames) {
            try {
                var fileContent = Files.readString(Path.of(targetDir, fileName));
                result.add((StorageItem<I, O>) mapper.deserialize(fileContent, getTypeReference()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    protected abstract TypeReference<?> getTypeReference();

    protected void write(StorageItem item) {
        try {
            var valueId = counter.getAndIncrement();
            var id = BaseStorage.padLeftZeros(String.valueOf(valueId), 10) + ".json";
            item.setIndex(valueId);
            var result = mapper.serialize(item);
            Files.writeString(Path.of(targetDir, id), result);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
