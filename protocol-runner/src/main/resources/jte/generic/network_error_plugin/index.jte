@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var pluginSettings = (org.kendar.plugins.settings.NetworkErrorPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.plugins.settings.NetworkErrorPluginSettings.class);
}

<script type="text/javascript">
    function updateData() {
        var data = retrieveValues({
            active:${pluginSettings.isActive()},
            percentAction: 0
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status, response) => {
            location.reload();
        })
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingSettingsError">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSettingsError"
               onclick="toggleAccordion('collapseSettingsError')"
               aria-expanded="true" aria-controls="collapseSettingError">
                Network error plugin settings
            </a>
        </h4>
    </div>
    <div id="collapseSettingsError" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSettingsError">
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
        </div>
    </div>
</div>