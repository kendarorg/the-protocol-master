package org.kendar.storage;

import org.kendar.di.annotations.TpmService;
import org.kendar.storage.generic.LineToWrite;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@TpmService(tags = "storage_null")
public class NullStorageRepository implements StorageRepository {
    private final AtomicLong counter = new AtomicLong(0);

    public NullStorageRepository() {

    }

    @Override
    public void initialize() {

    }

    @Override
    public void write(LineToWrite lineToWrite) {

    }

    @Override
    public void finalizeWrite(String instanceId) {

    }

    @Override
    public StorageItem readById(String instanceId, long id) {
        return null;
    }

    @Override
    public List<StorageItem> readResponses(String instanceId, ResponseItemQuery query) {
        return List.of();
    }

    @Override
    public byte[] readAsZip() {
        return new byte[0];
    }

    @Override
    public void writeZip(byte[] byteArray) {

    }

    @Override
    public long generateIndex() {
        return counter.incrementAndGet();
    }

    @Override
    public List<CompactLineComplete> getAllIndexes(int maxLen) {
        return List.of();
    }

    @Override
    public List<CompactLine> getIndexes(String instanceId) {
        return List.of();
    }

    @Override
    public void clean() {

    }

    @Override
    public void update(long itemId, String protocolInstanceId, CompactLine index, StorageItem item) {

    }

    @Override
    public void delete(String instanceId, long itemId) {

    }

    @Override
    public List<StorageFileIndex> listPluginFiles(String instanceId, String pluginId) {
        return List.of();
    }

    @Override
    public StorageFile readPluginFile(StorageFileIndex index) {
        return null;
    }

    @Override
    public void writePluginFile(StorageFile file) {

    }

    @Override
    public void delPluginFile(StorageFileIndex storageFileIndex) {

    }

    @Override
    public List<String> listFiles() {
        return List.of();
    }

    @Override
    public List<String> listInstanceIds() {
        return List.of();
    }

    @Override
    public List<String> listPluginIds(String instanceId) {
        return List.of();
    }

    @Override
    public String getType() {
        return "storage";
    }


}
