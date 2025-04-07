@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = ((org.kendar.http.HttpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.http.HttpProtocolSettings.class));
    var pluginSettings =((org.kendar.http.plugins.settings.HttpLatencyPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.http.plugins.settings.HttpLatencyPluginSettings.class));
    var targetsContainer = new org.kendar.http.plugins.apis.dtos.TargetsContainer(pluginSettings.getTarget(),"updateData");
}

<script type="text/javascript">
    function updateData() {
        var data = retrieveValues({
            active:${pluginSettings.isActive()},
            minMs: 0,
            maxMs: 0,
            percentAction: 0,
            target: pluginTargets
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status, response) => {
            location.reload();
        })
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingSpecificPlugin">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSpecificPlugin"
               onclick="toggleAccordion('collapseSpecificPlugin')"
               aria-expanded="true" aria-controls="collapseSpecificPlugin">
                Latency plugin settings
            </a>
        </h4>
    </div>
    <div id="collapseSpecificPlugin" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSpecificPlugin">
        <div class="panel-body">
            <div class="input-group">
                <button class="btn btn-danger" type="button"
                        onclick="updateData()">Upate settings
                </button>
            </div>
            <br>
            <div class="input-group">
                <label for="percentAction" class="control-label">Percent action</label>
                <div>
                    <input id="percentAction" type="number" class="form-control"
                           value="${pluginSettings.getPercentAction()}">
                </div>
            </div>
            <br>
            <div class="input-group">
                <label for="minMs" class="control-label">Min ms latency</label>
                <div>
                    <input id="minMs" type="number" class="form-control"
                           value="${pluginSettings.getMinMs()}">
                </div>
            </div>
            <br>
            <div class="input-group">
                <label for="maxMs" class="control-label">Max ms latency</label>
                <div>
                    <input id="maxMs" type="number" class="form-control"
                           value="${pluginSettings.getMaxMs()}">
                </div>
            </div>
            <br>
            @template.http.targets(targetsContainer)

        </div>
    </div>
</div>