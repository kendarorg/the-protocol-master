package org.kendar.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Base class for the storage
 *
 * @param <I>
 * @param <O>
 */
public abstract class BaseStorage<I, O> implements Storage<I, O> {
    protected static final JsonMapper mapper = new JsonMapper();
    protected boolean useFullData = false;
    protected HashSet<Integer> completedIndexes = new HashSet<>();
    protected HashSet<Integer> completedOutIndexes = new HashSet<>();
    private final StorageRepository<I, O> repository;

    public abstract String getCaller();

    public BaseStorage(StorageRepository<I, O> repository) {

        this.repository = repository;
    }

    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    public void write(int connectionId, I request, O response, long durationMs, String type, String caller) {
        var item = new StorageItem(connectionId, request, response, durationMs, type, getCaller());
        write(item);
    }

    public void write(long index, int connectionId, I request, O response, long durationMs, String type, String caller) {
        var item = new StorageItem(index, connectionId, request, response, durationMs, type, getCaller());
        write(item);
    }

    public StorageItem beforeSendingReadResult(StorageItem<I,O> si, CompactLine compactLine) {
        return si;
    }

    protected void write(StorageItem item){
        item.setCaller(getCaller());
        repository.write(item);
    }

    public Storage<I, O> withFullData() {
        this.useFullData = true;
        return this;
    }

    public boolean useFullData(){return  useFullData;}

    public abstract TypeReference<?> getTypeReference();

    public long generateIndex() {
        return ProtoDescriptor.getCounter("STORAGE_ID");
    }

    /**
     * Initialize (prepare the directory)
     */
    @Override
    public void initialize() {
        repository.initialize(this);
    }

    @Override
    public void optimize() {
        repository.optimize();
    }

    public abstract boolean shouldNotSave(CompactLine cl, List<CompactLine> compactLines, StorageItem<I, O> item, List<StorageItem<I, O>> loadedData);

    public abstract Map<String, String> buildTag(StorageItem<I, O> item);

    public StorageItem<I,O> read(I node, String type) {
        var query = new CallItemsQuery();
        query.setCaller(getCaller());
        query.setType(type);
        query.setUsed(completedIndexes);
        return read(query);
    }



    @Override
    public List<StorageItem<I, O>> readResponses(long afterIndex) {
        var respQuery = new ResponseItemQuery();
        respQuery.setCaller(getCaller());
        respQuery.setUsed(completedOutIndexes);
        respQuery.setStartAt(afterIndex);
        var responses = repository.readResponses(respQuery);
        var result = new ArrayList<StorageItem<I, O>>();
        for(var response: responses) {
            completedOutIndexes.add((int) response.getIndex());
            result.add(response);
        }
        return result;
    }

    public StorageItem read(CallItemsQuery query) {
            query.setUsed(completedIndexes);
            query.setCaller(getCaller());
            var result = repository.read(query);
            if (result != null) {
                completedIndexes.add((int) result.getIndex());
            }
            return result;
    }


}
