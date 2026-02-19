@import org.kendar.di.DiService
@import org.kendar.plugins.JdbcForwardMatcher
@import org.kendar.settings.GlobalSettings
@import org.kendar.settings.JdbcRewritePluginSettings
@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = DiService.getThreadContext().getInstance(GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var pluginSettings = (JdbcRewritePluginSettings)protocolSettings.getPlugin(data.getId(),JdbcRewritePluginSettings.class);
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
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings',
            'PUT',
            JSON.stringify(data),
            'application/json',
            (status, response) => {
            if(status==200){
                document.location.reload();
            }
        })
    }
    function removeInterceptor(id){
        /*sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/mappings/'+id,
            'DELETE',null,'application/json',
            (status,response)=>{
                if(status==200){
                    document.location.reload();
                }
            });*/
    }
    function addInterceptor(){
        /*var source = document.getElementById('source').value;
        var target = document.getElementById('target').value;
        var id = document.getElementById('id').value;
        const params = {
            source: source,
            target: target,
            id: id
        };
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/test?'+params.toString(),
            'GET',null,'application/json',
            (status,response)=>{
                document.getElementById("targetSample").value= JSON.parse(response);
            });*/
    }

    function testMatching(){
        var source = document.getElementById('source').value;
        var target = document.getElementById('target').value;
        var sourceSample = document.getElementById('sourceSample').value;
        const params = new URLSearchParams({
            source: source,
            target: target,
            data: sourceSample
        });
        sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/test?'+params.toString(),
            'GET',null,'application/json',
            (status,response)=>{
                document.getElementById("targetSample").value= JSON.parse(response);
            });
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingSettingsError">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSettingsError"
               onclick="toggleAccordion('collapseSettingsError')"
               aria-expanded="true" aria-controls="collapseSettingsError">
                Jdbc Forward Plugin
            </a>
        </h4>
    </div>
    <div id="collapseSettingsError" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSettingsError">
        <div class="panel-body">
            <br>
            <table class="table table-bordered table-striped">
                <tr>
                    <th>Id</th>
                    <th>Source</th>
                    <th>Target</th>
                    <th></th>
                </tr>
                @for(var element : pluginSettings.getMappings().entrySet().stream().map(m->new JdbcForwardMatcher(m.getKey(),m.getValue())).toList())
                    <tr>
                        <td>${element.getId()}</td>
                        <td>${element.getOriSource()}</td>
                        <td>${element.getOriTarget()}</td>

                        <td>
                            <button class="btn btn-danger" type="button"
                                    onclick="removeInterceptor('${element.getId()}')">Remove
                            </button>
                        </td>
                    </tr>
                @endfor
            </table>
            <br>
            <input id="id" type="hidden" value=""/>
            <table>
                <tr><td>
                        <div class="input-group ">
                            <label for="source" class="control-label">Source</label>
                            <div>
                                <input id="source" type="text" class="form-control"/>
                            </div>
                        </div>
                    </td><td>&nbsp;</td><td>
                        <div class="input-group ">
                            <label for="sourceSample" class="control-label">Source Sample</label>
                            <div>
                                <input id="sourceSample" type="text" class="form-control"/>
                            </div>
                        </div>
                    </td></tr>
                <tr><td>

                        <div class="input-group ">
                            <label for="target" class="control-label">Target</label>
                            <div>
                                <input id="target" type="text" class="form-control"/>
                            </div>
                        </div>


                    </td><td>&nbsp;</td><td>
                        <div class="input-group ">
                            <label for="targetSample" class="control-label">Target Result</label>
                            <div>
                                <input readonly="true" id="targetSample" type="text" class="form-control"/>
                            </div>
                        </div>


                    </td></tr>
            </table>

            <br>
            <div class="input-group">
                <button type="button" class="btn  btn-success"
                        onclick="addInterceptor()">
                    Insert
                </button>&nbsp;
                <button type="button" class="btn  btn-success"
                        onclick="testMatching()">
                    Test
                </button>
            </div>
            <br>
        </div>
    </div>

</div>
