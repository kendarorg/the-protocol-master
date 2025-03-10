package org.kendar.storage.generic;

import org.kendar.Service;
import org.kendar.di.DiService;
import org.kendar.events.EventsQueue;
import org.kendar.events.StorageReloadedEvent;
import org.kendar.settings.GlobalSettings;
import org.kendar.storage.CompactLine;
import org.kendar.storage.CompactLineComplete;
import org.kendar.storage.PluginFileManager;
import org.kendar.storage.StorageItem;
import org.kendar.utils.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class StorageRepository implements Service {
    private final DiService diService;
    private final JsonMapper mapper;
    protected static final Logger log = LoggerFactory.getLogger(StorageRepository.class);

    public StorageRepository(DiService diService, JsonMapper mapper) {
        this.diService = diService;
        this.mapper = mapper;
    }
    public PluginFileManager buildPluginFileManager(String instanceId,String pluginId) {
        return new PluginFileManager(this,instanceId,pluginId);
    }

    public abstract void deleteRecording(String protocolInstanceId, long itemId);
    public abstract void updateRecording(long itemId, String protocolInstanceId, CompactLine index, StorageItem item);
    public abstract List<String> listDirs(String ... path);
    public abstract String readFile(String ... path);
    public abstract List<String> listFiles(String ... path);
    public abstract void writeFile(String content,String ... path);
    public abstract void deleteFile(String ... path);

    public abstract void initialize();

    public String getSettings() {
        return readFile("settings");
    }

    public abstract StorageItem readFromScenarioById(String instanceId, long id);

    public abstract List<StorageItem> readResponsesFromScenario(String instanceId, ResponseItemQuery query);


    protected void zipFile(String fileName, ZipOutputStream zipOut) throws IOException {
        var content = this.readFile(fileName).getBytes();
        ZipEntry zipEntry = new ZipEntry(fileName+".json");
        zipOut.putNextEntry(zipEntry);
        zipOut.write(content, 0, content.length);
    }

    protected void zipDir(String fileName, ZipOutputStream zipOut) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName+"/");
        zipOut.putNextEntry(zipEntry);
        zipOut.closeEntry();
        for(var item:this.listFiles(fileName)) {
            zipFile(fileName+"/"+item,zipOut);
        }
    }

    public byte[] readAsZip() {
        var baos = new ByteArrayOutputStream();
        var globalSettings = diService.getInstance(GlobalSettings.class);
        var globalSettingsFile = mapper.serialize(globalSettings);
            writeFile("settings", globalSettingsFile);

        try (var zos = new ZipOutputStream(baos)) {
            for(var item:this.listFiles()) {
                zipFile(item,zos);
            }
            for(var item:this.listDirs()){
                zipDir(item,zos);
            }
            /*(for (var file : new File(Path.of(targetDir).toAbsolutePath().toString()).listFiles()) {
                zipFile(file, file.getName(), zos);
            }*/
        } catch (IOException ioe) {
            log.error("Error Creating storage zip", ioe);
        }
        return baos.toByteArray();
    }

    protected static File createNewFileFromZip(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
    public void writeZip(byte[] byteArray) {
//        String settingsPath = null;
//        var destDirString = Path.of(targetDir).toAbsolutePath().toString();
      File destDir = new File("");
//        // create output directory if it doesn't exist
//        if (!destDir.exists()) destDir.mkdirs();
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

                File newFile = createNewFileFromZip(destDir, zipEntry);
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
                    var fos = new ByteArrayOutputStream();
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    writeFile(new String(fos.toByteArray()),newFile.getPath());
                }
                zipEntry = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
            EventsQueue.send(new StorageReloadedEvent().withSettings("settings"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract long generateIndex();


    public abstract List<CompactLineComplete> getAllIndexes(int maxLen);

    public abstract List<CompactLine> getIndexes(String instanceId);

    public abstract void clean();

}
