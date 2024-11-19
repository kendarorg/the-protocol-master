package org.kendar.storage.generic;

import org.kendar.Service;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface StorageRepository extends Service {
    void initialize();

    void write(LineToWrite lineToWrite);

    void finalizeWrite(String instanceId);

    LineToRead read(String instanceId, CallItemsQuery query);

    List<StorageItem> readResponses(String instanceId, ResponseItemQuery query);

    byte[] readAsZip();
}
