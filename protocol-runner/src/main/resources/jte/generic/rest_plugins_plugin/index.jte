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
            name: null,
            target: null,
            inputType: null,
            phase: null,
            inputMatcher: null,
            outputType: null,
            outputMatcher: null
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
            <br>
            <table class="table table-bordered table-striped">
                <tr>
                    <th>Name</th>
                    <th>Target</th>
                    <th>Phase</th>
                    <th>Input Type</th>
                    <th>Input Matcher</th>
                    <th>Output Type</th>
                    <th>Output Matcher</th>
                    <th></th>
                </tr>
                @for(var element : pluginSettings.getInterceptors())
                    <tr>
                        <td>${element.getName()}</td>
                        <td>${element.getDestinationAddress()}</td>
                        <td>${element.getPhase()}</td>
                        <td>${element.getInputType()}</td>
                        <td>${element.getInMatcher()}</td>
                        <td>${element.getOutputType()}</td>
                        <td>${element.getOutMatcher()}</td>

                        <td>
                            <button class="btn btn-danger" type="button"
                                    onclick="removeInterceptor('${element.getName()}')">Remove
                            </button>
                        </td>
                    </tr>
                @endfor
            </table>
            <br>
            <div class="input-group ">
                <label for="name" class="control-label">Name</label>
                <div>
                    <input id="fileName" type="text" class="form-control"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="destinationAddress" class="control-label">Destination Address</label>
                <div>
                    <input id="destinationAddress" type="text" class="form-control"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="phase" class="control-label">Name</label>
                <select id="contentType" class="form-control">
                    <option value="NONE" selected>NONE</option>
                    <option value="CONNECT">CONNECT</option>
                    <option value="PRE_CALL">PRE_CALL</option>
                    <option value="POST_CALL">POST_CALL</option>
                    <option value="FINALIZE">FINALIZE</option>
                    <option value="PRE_SOCKET_WRITE">PRE_SOCKET_WRITE</option>
                    <option value="ASYNC_RESPONSE">ASYNC_RESPONSE</option>
                </select>
            </div>
            <div class="input-group ">
                <label for="inputType" class="control-label">Input Type</label>
                <div>
                    <input id="inputType" type="text" class="form-control"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="inputMatcher" class="control-label">Input Matcher</label>
                <div>
                    <input id="inputMatcher" type="text" class="form-control"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="outputType" class="control-label">Output Type</label>
                <div>
                    <input id="outputType" type="text" class="form-control"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="outputMatcher" class="control-label">Output Matcher</label>
                <div>
                    <input id="outputMatcher" type="text" class="form-control"/>
                </div>
            </div>
            <br>
            <div class="input-group">
                <button type="button" class="btn  btn-success"
                        onclick="addInterceptor()">
                    Insert
                </button>
            </div>
            <br>
        </div>
    </div>

</div>