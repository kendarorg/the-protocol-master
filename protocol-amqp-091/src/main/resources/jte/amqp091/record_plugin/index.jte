@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var storage = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.storage.generic.StorageRepository.class);
    var files = storage.listFiles("scenario").stream().filter(f->f.endsWith("."+protocolSettings.getProtocolInstanceId())).toList();
    var pluginSettings = (org.kendar.plugins.settings.BasicAysncRecordPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.plugins.settings.BasicAysncRecordPluginSettings.class);
}

<script type="text/javascript">

    function updateData() {
        var data = retrieveValues({
            active:${data.isActive()},
            ignoreTrivialCalls:true,
            resetConnectionsOnStart:true
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status, response) => {
            location.reload();
        })
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingRecordSettings">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseRecordSettings"
               onclick="toggleAccordion('collapseRecordSettings')"
               aria-expanded="true" aria-controls="collapseRecordSettings">
                Record plugin settings
            </a>
        </h4>
    </div>
    <div id="collapseRecordSettings" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingRecordSettings">
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
                        @if(pluginSettings.isResetConnectionsOnStart())
                            <input id="resetConnectionsOnStart" type="checkbox" checked>
                        @else
                            <input id="resetConnectionsOnStart" type="checkbox">
                        @endif
                        Reset connections on start
                    </label>
                </div>
            </div>
            <br>
        </div>
    </div>
</div>