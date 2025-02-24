package org.kendar.storage.cli;

import org.kendar.di.annotations.TpmService;

@TpmService
public class FileStorageCli implements StorageCli {
    @Override
    public String getDescription() {
        return "file=[absolute or relative path] save on disk";
    }
}
