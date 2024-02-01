package org.kendar.amqp.v09;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.amqp.v09.utils.AmqpStorage;
import org.kendar.storage.BaseFileStorage;
import org.kendar.storage.StorageItem;

import java.nio.file.Path;

public class AmqpFileStorage extends BaseFileStorage<String,String> implements AmqpStorage {


    public AmqpFileStorage(String targetDir) {
        super(targetDir);
    }

    public AmqpFileStorage(Path targetDir) {
        super(targetDir);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<StorageItem<String, String>>() {
        };
    }
}
