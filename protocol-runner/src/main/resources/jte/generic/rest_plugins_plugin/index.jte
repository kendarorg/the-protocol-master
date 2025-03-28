@import org.kendar.plugins.settings.BasicRestPluginsPluginSettings
@import org.kendar.di.DiService
@import org.kendar.settings.GlobalSettings
@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = DiService.getThreadContext().getInstance(GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var pluginSettings = (BasicRestPluginsPluginSettings)protocolSettings.getPlugin(data.getId(),BasicRestPluginsPluginSettings.class);
}

<script type="text/javascript">
    function updateData() {
        var data = retrieveValues({
            active:${pluginSettings.isActive()},
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
               aria-expanded="true" aria-controls="collapseSettingsError">
                Rest Plugins Plugin
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
        </div>
    </div>
</div>>