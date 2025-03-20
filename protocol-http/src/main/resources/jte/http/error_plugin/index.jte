@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = ((org.kendar.http.HttpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.http.HttpProtocolSettings.class));
    var pluginSettings =((org.kendar.http.plugins.settings.HttpErrorPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.http.plugins.settings.HttpErrorPluginSettings.class));
    var targetsContainer = new org.kendar.http.plugins.apis.dtos.TargetsContainer(pluginSettings.getTarget(),"updateData");
}

<script type="text/javascript">
    function updateData() {
        var data = retrieveValues({
            active:${pluginSettings.isActive()},
            percentAction: 0,
            showError: 0,
            errorMessage: "ERROR",
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
                Error plugin settings
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
                <label for="showError" class="control-label">Http error code</label>
                <div>
                    <input id="showError" type="number" class="form-control"
                           value="${pluginSettings.getShowError()}">
                </div>
            </div>
            <br>
            <div class="input-group">
                <label for="errorMessage" class="control-label">Error message</label>
                <div>
                    <input id="errorMessage" type="text" class="form-control"
                           value="${pluginSettings.getErrorMessage()}">
                </div>
            </div>
            @template.http.targets(targetsContainer)

        </div>
    </div>
</div>