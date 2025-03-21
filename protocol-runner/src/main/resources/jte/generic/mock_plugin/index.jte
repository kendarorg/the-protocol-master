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


    function pasteData() {
        navigator.clipboard.readText()
            .then(text => {
                var obj = JSON.parse(text);
                setValue('type', obj['type']);
                setValue('index', obj['index']);
                setValue('caller', obj['caller']);
                setValue('inputType', obj['inputType']);
                setValue('outputType', obj['outputType']);
                setValue('nthRequest', obj['nthRequest']);
                setValue('count', obj['count']);
                setValue('output', JSON.stringify(obj['output']));
                setValue('input', JSON.stringify(obj['input']));
            })
            .catch(err => {
                console.error('Failed to read clipboard contents: ', err);
                alert("Invalid content! " + err);
            });
    }

    function saveContentFile() {
        var toSend = retrieveValues(
            {
                connectionId: 0,
                index: -1,
                durationMs: 0,
                type: null,
                caller: null,
                inputType: null,
                outputType: null,
                timestamp: 0,
                constant: false,
                nthRequest: -1,
                count: -1
            });
        try {
            toSend.input = JSON.parse(retrieveValue("input"));
        } catch (error) {
            toSend.input = {};
        }
        try {
            toSend.output = JSON.parse(retrieveValue("output"));
        } catch (error) {
            toSend.output = {};
        }
        console.log("Copying record");
        console.log(toSend);
        var fileName = retrieveValue("fileName");
        var path = "/api/protocols/${data.getInstanceId()}/plugins/mock-plugin/mocks/" + fileName;
        sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status, message) => {
            if (status == 200) {
                alert("Data Sent!");
            } else {
                alert("Error sending data! " + message)
            }

        })
    }

    function deleteContentFile() {
        var filename = retrieveValue("fileName");
        getData('/api/protocols/${data.getInstanceId()}/plugins/mock-plugin/mocks/' + filename, 'DELETE', () => {
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
                Mock plugin settings
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