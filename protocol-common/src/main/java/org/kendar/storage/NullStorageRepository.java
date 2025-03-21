package org.kendar.storage;

import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmService;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@TpmService(tags = "storage_null")
public class NullStorageRepository extends StorageRepository {
    private final AtomicLong counter = new AtomicLong(0);

    public NullStorageRepository() {
        super(null, null);
    }

    public NullStorageRepository(DiService diService, JsonMapper mapper) {
        super(diService, mapper);
    }

    @Override
    public void deleteRecording(String protocolInstanceId, long itemId) {

    }

    @Override
    public void updateRecording(long itemId, String protocolInstanceId, CompactLine index, StorageItem item) {

    }


    public String getType() {
        return "storage";
    }

    @Override
    public List<String> listDirs(String... path) {
        return List.of();
    }

    @Override
    public String readFile(String... path) {
        return "";
    }

    @Override
    public List<String> listFiles(String... path) {
        return List.of();
    }

    @Override
    public void writeFile(String content, String... path) {

    }

    @Override
    public void deleteFile(String... path) {

    }

    @Override
    public void initialize() {

    }

    @Override
    public StorageItem readFromScenarioById(String instanceId, long id) {
        return null;
    }

    @Override
    public List<StorageItem> readResponsesFromScenario(String instanceId, ResponseItemQuery query) {
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
        return 0;
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
}
