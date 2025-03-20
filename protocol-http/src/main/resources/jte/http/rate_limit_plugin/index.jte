@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = ((org.kendar.http.HttpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.http.HttpProtocolSettings.class));
    var pluginSettings =((org.kendar.http.plugins.settings.HttpRateLimitPluginSettings)protocolSettings.getPlugin(data.getId(),org.kendar.http.plugins.settings.HttpRateLimitPluginSettings.class));
    var targetsContainer = new org.kendar.http.plugins.apis.dtos.TargetsContainer(pluginSettings.getTarget(),"updateData");
}

<script type="text/javascript">
    function updateData(){
        var data = retrieveValues({
            active:${pluginSettings.isActive()},
            percentAction:0,
            headerLimit:null,
            headerRemaining:null,
            headerReset:null,
            headerRetryAfter:0,
            costPerRequest:0,
            resetTimeWindowSeconds:0,
            warningThresholdPercent:0,
            rateLimit:0,
            resetFormat:0,
            useCustomResponse:false,
            target:pluginTargets
        });
        console.log(data);
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings', 'PUT', JSON.stringify(data), 'application/json', (status,response) => {
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
            </div><br>
            <div class="input-group">
                <label for="percentAction" class="control-label">Percent action</label>
                <div>
                    <input id="percentAction" type="number" class="form-control"
                           value="${pluginSettings.getPercentAction()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="headerLimit" class="control-label">Header limit</label>
                <div>
                    <input id="headerLimit" type="text" class="form-control"
                           value="${pluginSettings.getHeaderLimit()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="headerRemaining" class="control-label">Header remaining</label>
                <div>
                    <input id="headerRemaining" type="text" class="form-control"
                           value="${pluginSettings.getHeaderRemaining()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="headerReset" class="control-label">Header reset</label>
                <div>
                    <input id="headerReset" type="text" class="form-control"
                           value="${pluginSettings.getHeaderReset()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="headerRetryAfter" class="control-label">Header retry after</label>
                <div>
                    <input id="headerRetryAfter" type="text" class="form-control"
                           value="${pluginSettings.getHeaderRetryAfter()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="costPerRequest" class="control-label">Cost per request</label>
                <div>
                    <input id="costPerRequest" type="number" class="form-control"
                           value="${pluginSettings.getCostPerRequest()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="resetTimeWindowSeconds" class="control-label">Reset time window seconds</label>
                <div>
                    <input id="resetTimeWindowSeconds" type="number" class="form-control"
                           value="${pluginSettings.getResetTimeWindowSeconds()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="warningThresholdPercent" class="control-label">Warning threshold percent</label>
                <div>
                    <input id="warningThresholdPercent" type="number" class="form-control"
                           value="${pluginSettings.getWarningThresholdPercent()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="rateLimit" class="control-label">Rate limit</label>
                <div>
                    <input id="rateLimit" type="number" class="form-control"
                           value="${pluginSettings.getRateLimit()}">
                </div>
            </div><br>
            <div class="input-group">
                <label for="resetFormat" class="control-label">Reset format (secondsLeft/UtcEpochSeconds)</label>
                <div>
                    <input id="resetFormat" type="number" class="form-control"
                           value="${pluginSettings.getResetFormat()}">
                </div>
            </div><br>
            @template.http.targets(targetsContainer)
            <br>
            <div class="input-group">
                <button class="btn btn-danger" type="button"
                        onclick="updateData()">Upate settings
                </button>
            </div><br>

        </div>
    </div>
</div>