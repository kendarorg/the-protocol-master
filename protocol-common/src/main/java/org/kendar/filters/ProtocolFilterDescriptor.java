package org.kendar.filters;

import org.kendar.storage.StorageItem;

import java.util.List;

public interface ProtocolFilterDescriptor extends FilterDescriptor{
    List<ProtocolPhase> getPhases();



    /**
     * @param phase
     * @param request
     * @param response
     * @return true when is blocking
     */
    boolean handle(ProtocolPhase phase, StorageItem in, StorageItem out);
}
