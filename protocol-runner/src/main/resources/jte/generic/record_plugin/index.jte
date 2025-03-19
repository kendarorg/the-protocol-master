@param org.kendar.ui.dto.SinglePluginDto data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var protocolSettings = globalSettings.getProtocolForKey(data.getInstanceId());
    var storage = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.storage.generic.StorageRepository.class);
    var files = storage.listFiles("scenario").stream().filter(f->f.endsWith("."+protocolSettings.getProtocolInstanceId())).toList();
}

<script type="text/javascript">
    function openSpecificFile(file) {
        return htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/${data.getId()}/file/' + file, {
            target: '#singleFile',
            swap: 'outerHTML'
        })
    }

    function searchFiles(tpmql){
        return htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/${data.getId()}/file?'+
            "tpmql="+encodeURIComponent(tpmql), {
            target: '#filesList',
            swap: 'outerHTML'
        })
    }

    function saveContentFile() {
        var filename = retrieveValue("currentFileName");
        var data = retrieveValue('body');
        sendData('/api/protocols/${data.getInstanceId()}/plugins/record-plugin/record/' + filename, 'POST',
            data, 'application/json', () => {
                location.reload()
            })
    }

    function deleteContentFile() {
        getData('/api/protocols/${data.getInstanceId()}/plugins/record-plugin/record/' + filename, 'DELETE', () => {
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
                Recorded data
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
                                            onclick="openSpecificFile(retrieveValue('createNewFile'))">
                                        Create</button>
                                </span>
                                <input id="createNewFile" type="text" size="30"
                                       class="form-control">
                            </div>
                            <br>
                            <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button" onclick="searchFiles(retrieveValue('searchFiles'))">
                                        Search</button>
                                </span>
                                <input id="searchFiles" type="text"
                                       class="form-control">
                            </div>
                            <br>
                            <div id="filesList" hx-get="/protocols/${data.getInstanceId()}/plugins/record-plugin/file"
                                 target="outerHTML"
                                 hx-trigger="load">
                            </div>

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