package org.kendar.utils;

import org.junit.jupiter.api.Test;
import org.kendar.storage.EncryptedStorageRepository;
import org.kendar.storage.StorageFile;
import org.kendar.storage.StorageFileIndex;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EncryptedStorageTest {
    @Test
    void testEncryptedStorage() throws NoSuchFieldException, IllegalAccessException, IOException {

        var repopath = Path.of("target", "EncryptedStorageTest", "testEncryptedStorage");
        var resultpath = Path.of("target", "EncryptedStorageTest", "testEncryptedStorage", "instance", "plugin", "file.json");
        var target = new EncryptedStorageRepository(repopath) {
            @Override
            protected String getEncriptionKey() {
                return "testEncryptedStorage";
            }
        };
        var index = new StorageFileIndex("instance", "plugin", "file");
        var sf = new StorageFile(index, "test");
        target.writePluginFile(sf);
        System.out.println(sf);
        var written = Files.readAllBytes(resultpath);
        var prologue = "ENCRYPTED".getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < prologue.length; i++) {
            var by = prologue[i];
            if (written[i] != by) {
                throw new RemoteException("Wrong prologue");
            }
        }

        var decrypt = target.readPluginFile(index);
        assertEquals("test", decrypt.getContent());
    }
}
