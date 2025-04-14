@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var storage = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.storage.generic.StorageRepository.class);
    var files = storage.listFiles(data.getInstanceId(),data.getId());
}

<script type="text/javascript">
    function openSpecificFile(file) {
        return htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/${data.getId()}/file/' + file, {
            target: '#singleFile',
            swap: 'outerHTML'
        })
    }

    function saveContentFile() {
        var filename = retrieveValue("fileName");
        var data = retrieveValues({
            toFind: null,
            toReplace: null,
            regex: false
        });
        sendData('/api/protocols/${data.getInstanceId()}/plugins/rewrite-plugin/rewrite/' + filename, 'POST',
            JSON.stringify(data), 'application/json', () => {
                location.reload()
            })
    }

    function testMatching() {
        var filename = retrieveValue("currentFileName");
        var data = retrieveValues({
            toFind: null,
            toReplace: null,
            testTarget: null,
            regex: false
        });
        sendData('/api/protocols/${data.getInstanceId()}/plugins/rewrite-plugin/test', 'POST',
            JSON.stringify(data), 'application/json', (code, result) => {
               showNotification("info","Result",result);
            })
    }

    function deleteContentFile() {
        var filename = retrieveValue("fileName");
        getData('/api/protocols/${data.getInstanceId()}/plugins/rewrite-plugin/rewrite/' + filename, 'DELETE', () => {
            location.reload()
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
                Rewrite plugin settings
            </a>
        </h4>
    </div>
    <div id="collapseSpecificPlugin" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSpecificPlugin">
        <div class="panel-body">
            <div class="container-fluid">
                <div class="col-xs-4">

                    <div class="panel panel-default">
                        <div class="panel-body">
                            <div class="input-group">
                        <span class="input-group-btn">
                            <button class="btn btn-default" type="button"
                                    onclick="openSpecificFile(retrieveValue('createNewFile')).then(()=>location.reload())">
                                Create</button>
                        </span>
                                <input id="createNewFile" type="text" size="30"
                                       class="form-control">
                            </div>
                            <br>
                            <table class="table table-bordered table-striped">
                                <tr>
                                    <th>Replace Id</th>
                                </tr>
                                @for(var file:files)
                                    <tr>
                                        <td><span onclick="openSpecificFile('${file}')">${file}</span></td>
                                    </tr>
                                @endfor
                            </table>
                        </div>
                    </div>

                </div>
                <div class="col-xs-8">

                    <div class="panel panel-default">
                        <div class="panel-body">
                            <div id="singleFile"></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>