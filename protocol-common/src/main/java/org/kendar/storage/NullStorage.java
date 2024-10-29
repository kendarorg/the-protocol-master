package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.storage.generic.StorageRepository;

import java.util.List;
import java.util.Map;

/**
 * Do-nothing storage
 *
 * @param <I>
 * @param <O>
 */
public class NullStorage<I, O> extends BaseStorage<I, O> {
    public NullStorage(StorageRepository<I, O> repository) {
        super(repository);
    }

    @Override
    public String getCaller() {
        return "NULL";
    }

    @Override
    public TypeReference<?> getTypeReference() {
        return null;
    }

    @Override
    public boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<I, O> item, List<StorageItem<I, O>> loadedData) {
        return true;
    }

    @Override
    public Map<String, String> buildTag(StorageItem<I, O> item) {
        return Map.of();
    }

}
