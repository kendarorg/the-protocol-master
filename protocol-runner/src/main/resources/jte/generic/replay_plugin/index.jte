@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var storage = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.storage.generic.StorageRepository.class);
    var pluginSettings = (org.kendar.plugins.settings.BasicReplayPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.plugins.settings.BasicReplayPluginSettings.class);
}

<script type="text/javascript">

    function updateData() {
        var data = retrieveValues({
            active:${data.isActive()},
            ignoreTrivialCalls: true,
            blockExternal: true,
            respectCallDuration: false
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status, response) => {
            location.reload();
        })
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingReplaySettings">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseReplaySettings"
               onclick="toggleAccordion('collapseReplaySettings')"
               aria-expanded="true" aria-controls="collapseReplaySettings">
                Replay plugin settings
            </a>
        </h4>
    </div>
    <div id="collapseReplaySettings" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingReplaySettings">
        <div class="panel-body">
            <div class="input-group">
                <button class="btn btn-danger" type="button"
                        onclick="updateData()">Update settings
                </button>
            </div>
            <br>
            <div class="input-group">
                <div class="checkbox">
                    <label>
                        @if(pluginSettings.isIgnoreTrivialCalls())
                            <input id="ignoreTrivialCalls" type="checkbox" checked>
                        @else
                            <input id="ignoreTrivialCalls" type="checkbox">
                        @endif
                        Ignore trivial calls
                    </label>
                </div>
            </div>
            <br>
            <div class="input-group">
                <div class="checkbox">
                    <label>
                        @if(pluginSettings.isRespectCallDuration())
                            <input id="respectCallDuration" type="checkbox" checked>
                        @else
                            <input id="respectCallDuration" type="checkbox">
                        @endif
                        Respect calls duration
                    </label>
                </div>
            </div>
            <br>
            <div class="input-group">
                <div class="checkbox">
                    <label>
                        @if(pluginSettings.isBlockExternal())
                            <input id="blockExternal" type="checkbox" checked>
                        @else
                            <input id="blockExternal" type="checkbox">
                        @endif
                        Block External calls
                    </label>
                </div>
            </div>
            <br>
        </div>
    </div>
</div>
