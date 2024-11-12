package org.kendar.filters;

import org.kendar.filters.settings.BasicReplayPluginSettings;
import org.kendar.protocol.context.ProtoContext;
import org.kendar.proxy.FilterContext;
import org.kendar.storage.StorageItem;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.ResponseItemQuery;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;
import org.kendar.utils.Sleeper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class BasicReplayingPlugin  extends ProtocolPluginDescriptor<Object, Object>{
    protected static JsonMapper mapper = new JsonMapper();
    protected StorageRepository storage;
    protected boolean hasCallbacks(){
        return false;
    }


    protected Object getData(Object of) {
        return of;
    }

    protected HashSet<Integer> completedIndexes = new HashSet<>();
    protected HashSet<Integer> completedOutIndexes = new HashSet<>();

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, Object in, Object out) {
        if(isActive()){
            if(out==null) {
                sendAndForget(filterContext, in);
                return true;
            }else{
                sendAndExpect(filterContext,in,out);
                return true;
            }
        }
        return false;
    }

    protected void sendAndExpect(FilterContext filterContext, Object in, Object out) {
        var query = new CallItemsQuery();
        var context = filterContext.getContext();

        query.setCaller(filterContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        query.setUsed(completedIndexes);
        var lineToRead = storage.read(getInstanceId(), query);


        var item = lineToRead.getStorageItem();

        if(hasCallbacks()) {
            var afterIndex = item.getIndex();
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(filterContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            var responses = storage.readResponses(getInstanceId(), respQuery);
            var result = new ArrayList<StorageItem>();
            for (var response : responses) {
                completedOutIndexes.add((int) response.getIndex());
                result.add(response);
            }
            sendBackResponses(filterContext.getContext(), result);
        }
        var outputItem = item.getOutput();
        if (context.isUseCallDurationTimes()) {
            Sleeper.sleep(item.getDurationMs());
        }
        buildState(filterContext,context,in, outputItem, out);
    }

    protected void buildState(FilterContext filterContext, ProtoContext context, Object in, Object outputItem, Object aClass) {

    }

    protected void sendAndForget(FilterContext filterContext, Object in) {
        var query = new CallItemsQuery();

        query.setCaller(filterContext.getCaller());
        query.setType(in.getClass().getSimpleName());
        query.setUsed(completedIndexes);
        var lineToRead = storage.read(getInstanceId(), query);
        var item = lineToRead.getStorageItem();
        if(hasCallbacks()) {

            var afterIndex = item.getIndex();
            var respQuery = new ResponseItemQuery();
            respQuery.setCaller(filterContext.getCaller());
            respQuery.setUsed(completedOutIndexes);
            respQuery.setStartAt(afterIndex);
            var responses = storage.readResponses(getInstanceId(), respQuery);
            var result = new ArrayList<StorageItem>();
            for (var response : responses) {
                completedOutIndexes.add((int) response.getIndex());
                result.add(response);
            }
            sendBackResponses(filterContext.getContext(), result);
        }
    }

    protected abstract void sendBackResponses(ProtoContext context, List<StorageItem> result);

    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.PRE_CALL);
    }

    @Override
    public String getId() {
        return "replay-pluing";
    }

    @Override
    public void terminate() {

    }

    @Override
    public Class<?> getSettingClass() {
        return BasicReplayPluginSettings.class;
    }
}
