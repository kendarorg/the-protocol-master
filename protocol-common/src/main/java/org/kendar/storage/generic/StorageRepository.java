package org.kendar.storage.generic;

import org.kendar.Service;
import org.kendar.storage.CompactLine;
import org.kendar.storage.StorageItem;

import java.util.List;

public interface StorageRepository extends Service {
    void initialize();

    void write(LineToWrite lineToWrite);

    void finalizeWrite(String instanceId);

    StorageItem readById(String instanceId, long id);

    List<StorageItem> readResponses(String instanceId, ResponseItemQuery query);

    byte[] readAsZip();

    void writeZip(byte[] byteArray);

    long generateIndex();

    void isRecording(String instanceId, boolean b);
    void isReplaying(String instanceId, boolean b);

    List<CompactLine> getIndexes(String instanceId);
}
