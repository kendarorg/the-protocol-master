package org.kendar.storage.generic;

import org.kendar.Service;
import org.kendar.storage.*;

import java.util.List;

public interface StorageRepository extends Service {
    void initialize();

    String getSettings();

    void write(LineToWrite lineToWrite);

    void finalizeWrite(String instanceId);

    StorageItem readById(String instanceId, long id);

    List<StorageItem> readResponses(String instanceId, ResponseItemQuery query);

    byte[] readAsZip();

    void writeZip(byte[] byteArray);

    long generateIndex();


    List<CompactLineComplete> getAllIndexes(int maxLen);

    List<CompactLine> getIndexes(String instanceId);

    void clean();

    void update(long itemId, String protocolInstanceId, CompactLine index, StorageItem item);

    void delete(String instanceId, long itemId);

    List<StorageFileIndex> listPluginFiles(String instanceId, String pluginId);

    StorageFile readPluginFile(StorageFileIndex index);

    void writePluginFile(StorageFile file);

    void delPluginFile(StorageFileIndex storageFileIndex);

    List<String> listFiles();
    List<String> listInstanceIds();
    List<String> listPluginIds(String instanceId);
}
