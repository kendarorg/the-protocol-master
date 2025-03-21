package org.kendar.storage;

import org.kendar.storage.generic.StorageRepository;

import java.util.List;

public class PluginFileManager {
    private final StorageRepository repository;
    private final String instanceId;
    private final String pluginId;

    public PluginFileManager(StorageRepository repository, String instanceId, String pluginId) {
        this.repository = repository;
        this.instanceId = instanceId;
        this.pluginId = pluginId;
    }

    public StorageRepository getRepository() {
        return repository;
    }

    public List<String> listFiles() {
        return this.repository.listFiles(instanceId, pluginId);
    }

    public String readFile(String fileName) {
        return this.repository.readFile(instanceId, pluginId, fileName);
    }

    public void writeFile(String fileName, String content) {
        this.repository.writeFile(content, instanceId, pluginId, fileName);
    }

    public void deleteFile(String fileName) {
        this.repository.deleteFile(instanceId, pluginId, fileName);
    }
}
