package org.kendar.storage;

import org.bouncycastle.crypto.CryptoException;
import org.kendar.di.annotations.TpmService;
import org.kendar.settings.GlobalSettings;
import org.kendar.utils.Encryptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@TpmService(tags = "encrypted_file")
public class EncryptedStorageRepository extends FileStorageRepository {

    private final Encryptor encryptor;

    public EncryptedStorageRepository(String targetDir) {
        super(targetDir);
        encryptor = getEncryptor();
    }

    public EncryptedStorageRepository(Path targetDir) {
        super(targetDir);
        encryptor = getEncryptor();
    }

    public EncryptedStorageRepository(GlobalSettings settings) {
        super(settings);
        encryptor = getEncryptor();
    }

    private Encryptor getEncryptor() {
        var encryptionKey = System.getenv("ENCRYPTION_KEY");
        if (encryptionKey != null && !encryptionKey.isEmpty()) {
            return new Encryptor(encryptionKey.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    protected String getFileContent(Path of) throws IOException {
        if (encryptor == null) {
            return super.getFileContent(of);
        }
        try {
            return encryptor.decryptString(Files.readAllBytes(of));
        } catch (CryptoException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected void setFileContent(Path of, String s) throws IOException {
        if (encryptor == null) {
            super.setFileContent(of, s);
        } else {
            try {
                Files.write(of, encryptor.encryptString(s));
            } catch (CryptoException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
