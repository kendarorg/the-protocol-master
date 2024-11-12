package org.kendar.filters;

import org.kendar.filters.settings.BasicReplayPluginSettings;
import org.kendar.proxy.FilterContext;
import org.kendar.sql.jdbc.SelectResult;
import org.kendar.sql.jdbc.proxy.JdbcCall;
import org.kendar.sql.jdbc.storage.JdbcRequest;
import org.kendar.sql.jdbc.storage.JdbcResponse;
import org.kendar.storage.FileStorageRepository;
import org.kendar.storage.generic.CallItemsQuery;
import org.kendar.storage.generic.LineToRead;
import org.kendar.storage.generic.StorageRepository;
import org.kendar.utils.JsonMapper;

import java.util.HashSet;
import java.util.List;

public abstract class BasicJdbcReplayingPlugin extends ProtocolPluginDescriptor<JdbcCall, SelectResult> {
    protected static JsonMapper mapper = new JsonMapper();
    protected StorageRepository storage;
    protected HashSet<Integer> completedIndexes = new HashSet<>();
    protected HashSet<Integer> completedOutIndexes = new HashSet<>();

    protected Object getData(Object of) {
        return of;
    }

    @Override
    public boolean handle(FilterContext filterContext, ProtocolPhase phase, JdbcCall in, SelectResult out) {
        if (isActive()) {
            sendAndExpect(filterContext, in, out);
            return true;
        }
        return false;
    }

    protected void sendAndExpect(FilterContext filterContext, JdbcCall inObj, SelectResult outObj) {
        var in = (JdbcCall) inObj;
        var out = (SelectResult) outObj;
        var query = new CallItemsQuery();

        query.setCaller(filterContext.getCaller());
        query.setType("QUERY");
        query.addTag("parametersCount", in.getParameterValues().size());
        query.addTag("query", in.getQuery());
        query.setUsed(completedIndexes);
        var lineToRead = beforeSendingReadResult(storage.read(getInstanceId(), query));
        /*if ((lineToRead == null || lineToRead.getStorageItem() == null) ||
                in.getQuery().trim().toLowerCase().startsWith("set")) {
            out.setCount(0);
            out.setIntResult(true);
        } else {
            var source = (SelectResult) mapper.deserialize(lineToRead.getStorageItem().getOutput(),SelectResult.class);
            if(source==null){
                out.setCount(0);
                out.setIntResult(true);
            }else {
                out.fill(source);
            }
        }*/
        if (lineToRead != null && lineToRead.getStorageItem() != null
                && lineToRead.getStorageItem().getOutput() != null) {
            var source = lineToRead.getStorageItem().retrieveOutAs(JdbcResponse.class);
            out.fill(source.getSelectResult());
        } else if (lineToRead.getCompactLine() != null) {// if(in.getQuery().trim().toLowerCase().startsWith("set")){

            if (lineToRead.getCompactLine().getTags().get("isIntResult").equalsIgnoreCase("true")) {
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(true);
                resultset.setCount(Integer.parseInt(lineToRead.getCompactLine().getTags().get("resultsCount")));
                out.fill(resultset);
            } else if (in.getQuery().trim().toLowerCase().startsWith("set")) {
                System.out.println("a");
            }
        }
        /*if ((lineToRead == null || lineToRead.getStorageItem() == null
                || lineToRead.getStorageItem().getOutput() == null) ||
                in.getQuery().trim().toLowerCase().startsWith("set")) {
            out.setCount(0);
            out.setIntResult(true);
        } else {

        }*/
    }

    protected LineToRead beforeSendingReadResult(LineToRead lineToRead) {
        if (lineToRead == null) return null;
        var idx = lineToRead.getCompactLine();
        var si = lineToRead.getStorageItem();
        if (si != null) {
            return lineToRead;
        }
        if (idx != null) {
            JdbcResponse resp = new JdbcResponse();
            if (idx.getTags().get("isIntResult").equalsIgnoreCase("true")) {
                resp.setIntResult(Integer.parseInt(idx.getTags().get("resultsCount")));
                SelectResult resultset = new SelectResult();
                resultset.setIntResult(true);
                resultset.setCount(resp.getIntResult());
                resp.setSelectResult(resultset);
            }
            si.setOutput(resp);
            JdbcRequest req = new JdbcRequest();
            req.setQuery(idx.getTags().get("query"));
            si.setInput(req);
        }
        return lineToRead;
    }


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

    public BasicJdbcReplayingPlugin withStorage(FileStorageRepository storage) {
        this.storage = storage;
        return this;
    }
}
