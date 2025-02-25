package org.kendar.storage.cli;

import org.kendar.di.annotations.TpmService;

@TpmService
public class EncryptedStorageCli implements StorageCli {
    @Override
    public String getDescription() {
        return "encrypted=[absolute or relative path] save on disk encrypted\n" +
                "the key is in ENCRYPTION_KEY environment variable or can be\n" +
                "added encrypted=[path]&key=[key]";
    }
}
