@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var storage = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.storage.generic.StorageRepository.class);
    var pluginSettings = (org.kendar.plugins.settings.BasicRecordPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.plugins.settings.BasicRecordPluginSettings.class);
}

<script type="text/javascript">

    function updateData() {
        var data = retrieveValues({
            active:${data.isActive()},
            ignoreTrivialCalls: true
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status, response) => {
            if(status==200)location.reload();
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
        </div>
    </div>
</div>
