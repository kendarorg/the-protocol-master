package org.kendar.storage.cli;

import org.kendar.di.annotations.TpmService;

@TpmService
public class NullStorageCli implements StorageCli {
    @Override
    public String getDescription() {
        return "null=[anything] noop";
    }
}
