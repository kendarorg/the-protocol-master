package org.kendar.storage;

import org.bouncycastle.crypto.CryptoException;
import org.kendar.di.DiService;
import org.kendar.di.annotations.TpmConstructor;
import org.kendar.di.annotations.TpmService;
import org.kendar.exceptions.TPMException;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.Encryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Calendar;

@TpmService(tags = "storage_encrypted")
public class EncryptedStorageRepository extends FileStorageRepository {

    private Encryptor encryptor;
    private String password;

    public EncryptedStorageRepository(String targetDir) {
        super(targetDir);
        encryptor = getEncryptor();
    }

    public EncryptedStorageRepository(Path targetDir) {
        super(targetDir);
        encryptor = getEncryptor();
    }

    @TpmConstructor
    public EncryptedStorageRepository(GlobalSettings settings, DiService diService) {
        super(settings, diService);
    }

    protected void initializeStorageRepo(GlobalSettings settings, DiService diService) {
        this.diService = diService;
        var dataDir = settings.getDataDir();
        if (dataDir == null || dataDir.isEmpty()) {
            dataDir = Path.of("data",
                    Long.toString(Calendar.getInstance().getTimeInMillis())).toAbsolutePath().toString();
        } else {
            if (dataDir.contains("&")) {
                var spl = dataDir.split("&");
                var pwd = spl[1].split("=");
                this.password = pwd[1];
                dataDir = spl[0].split("=")[1];
            } else {
                var spl = dataDir.split("=");
                if (spl.length == 2) {
                    dataDir = spl[1];
                }
            }
        }
        encryptor = getEncryptor();
        this.targetDir = Path.of(dataDir).toAbsolutePath().toString();
        this.targetDir = ensureDirectory(targetDir);
    }

    private Encryptor getEncryptor() {
        var encryptionKey = getEncriptionKey();
        if (encryptionKey != null && !encryptionKey.isEmpty()) {
            return new Encryptor(encryptionKey.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    protected String getEncriptionKey() {
        if (this.password != null && !this.password.isEmpty()) {
            return this.password;
        }
        return System.getenv("ENCRYPTION_KEY");
    }

    @Override
    protected String getFileContent(Path of) throws IOException {
        if (encryptor == null) {
            return super.getFileContent(of);
        }
        try {
            var result = Files.readAllBytes(of);
            var prologue = "ENCRYPTED".getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < prologue.length; i++) {
                var by = prologue[i];
                if (result[i] != by) {
                    return super.getFileContent(of);
                }
            }
            var toDecrypt = Arrays.copyOfRange(result, prologue.length, result.length);
            return encryptor.decryptString(toDecrypt);
        } catch (CryptoException e) {
            throw new TPMException(e);
        }
    }


    @Override
    protected void setFileContent(Path of, String s) throws IOException {
        if (encryptor == null) {
            super.setFileContent(of, s);
        } else {
            try {
                var encryptedData = encryptor.encryptString(s);
                Files.writeString(of, "ENCRYPTED");
                Files.write(of, encryptedData, StandardOpenOption.APPEND);
            } catch (CryptoException e) {
                throw new TPMException(e);
            }
        }

    }
}
