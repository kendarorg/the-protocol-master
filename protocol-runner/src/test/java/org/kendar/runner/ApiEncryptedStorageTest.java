package org.kendar.runner;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kendar.Main;
import org.kendar.plugins.apis.Ok;
import org.kendar.utils.Sleeper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ApiEncryptedStorageTest extends ApiTestBase {
    private static BasicTest bs;

    @AfterAll
    public static void cleanup() {
        Main.stop();
        Sleeper.sleep(1000);
    }

    @BeforeAll
    public static void setup() {
        try {
            Main.stop();
        } catch (Exception e) {
        }
        Sleeper.sleep(1000);
        var args = new String[]{

                "-cfg", Path.of("src", "test", "resources", "apitestsencstorage.json").toString()
        };
        bs = new BasicTest();
        bs.startAndHandleUnexpectedErrors(args);
        Sleeper.sleep(3000);
    }

    protected static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    @Test
    void globalApiTest() throws Exception {

        var httpclient = HttpClients.createDefault();
        var data = Files.readAllBytes(Path.of("src", "test", "resources", "testcontent.zip"));
        var okResult = postRequest("http://localhost:5005/api/global/storage", httpclient, data, new TypeReference<Ok>() {
        }, "application/zip");
        assertEquals("OK", okResult.getResult());
        assertThrows(MalformedInputException.class, () -> Files.readString(Path.of("target", "tests", "encrypted", "index.http-01.json")));
        var zip = downloadRequest("http://localhost:5005/api/global/storage", httpclient);
        assertTrue(zip.length > 100);
        Files.write(Path.of("target", "downloaded.zip"), zip);
        var expectedFiles = getPaths(data).stream().filter(f->f.endsWith(".json")).sorted().toList();
        var testedFiles = getPaths(zip).stream().sorted().toList();
        assertArrayEquals(expectedFiles.toArray(), testedFiles.toArray());

    }

    private List<String> getPaths(byte[] data) {
        try {
            File destDir = new File("test");
            byte[] buffer = new byte[1024];
            var result = new ArrayList<String>();
            var fis = new ByteArrayInputStream(data);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry == null) {
                throw new RuntimeException("Not a zip file!");
            }
            while (zipEntry != null) {
                /*ZIPSETTINGS if(zipEntry.getName().equalsIgnoreCase("settings.json") &&
                        Path.of(targetDir).toAbsolutePath().compareTo(destDir.toPath().toAbsolutePath()) == 0) {
                    settingsDir =Path.of(destDir.getAbsolutePath(),zipEntry.getName()).toString();
                }*/
                File newFile = newFile(destDir, zipEntry);
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

                    result.add(newFile.toPath().toString());

                }
                zipEntry = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
