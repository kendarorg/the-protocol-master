@param org.kendar.settings.ProtocolSettings protocolSettings

!{
    var protocol = (org.kendar.dns.DnsProtocolSettings)protocolSettings;
    var safeId = new java.util.function.Function<org.kendar.settings.ProtocolSettings,String>(){
            @Override
            public String apply(org.kendar.settings.ProtocolSettings protocolSettings) {
                return protocolSettings.getProtocolInstanceId().replaceAll("-","");
            }
        };
}
<script type="text/javascript">
    function deleteRegistered(name) {
        var path = "/api/protocols/${protocol.getProtocolInstanceId()}/dns/registered/" + name;
        getData(path, 'DELETE', () => {
            location.reload()
        })
    }

    function deleteBlocked(name) {
        var path = "/api/protocols/${protocol.getProtocolInstanceId()}/dns/blocked/" + name;
        getData(path, 'DELETE', () => {
            location.reload()
        })
    }

    function saveRegistered(toSend) {
        console.log("Sending");
        console.log(toSend);
        var path = "/api/protocols/${protocol.getProtocolInstanceId()}/dns/registered";
        sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status) => {
            if (status == 200) {
                alert("Data Sent!");
                location.reload();
            } else {
                alert("Error sending data!")
            }

        })
    }

    function createNewRegistered${safeId.apply(protocol)}() {
        var name = retrieveValue('newRegistered${safeId.apply(protocol)}');
        var ip = retrieveValue('newRegisteredIp${safeId.apply(protocol)}');
        if (ip === null || ip === "") {
            ip = "127.0.0.1";
        }
        var toSend = [
            {name: name, ip: ip}
        ];
        saveRegistered(toSend);
    }

    function createNewBlocked${safeId.apply(protocol)}() {
        var name = retrieveValue('newBlocked${safeId.apply(protocol)}');
        var toSend = [
            name
        ];
        console.log("Sending");
        console.log(toSend);
        var path = "/api/protocols/${protocol.getProtocolInstanceId()}/dns/blocked";
        sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status) => {
            if (status == 200) {
                alert("Data Sent!");
                location.reload();
            } else {
                alert("Error sending data!")
            }

        })
    }

    function loadFromClipboard${safeId.apply(protocol)}() {
        navigator.clipboard.readText()
            .then(text => {
                var toSend = [];
                var obj = JSON.parse(text);
                for (let i = 0; i < obj.length; i++) {
                    let objName = obj[i]['name'];
                    toSend.push({
                        name: objName,
                        ip: "127.0.0.1",
                        timestamp: 0
                    })
                }
                saveRegistered(toSend);
            })
            .catch(err => {
                console.error('Failed to read clipboard contents: ', err);
                alert("Invalid content! " + err);
            });
    }

    function updateData${safeId.apply(protocol)}() {
        if (confirm("You will be restarting the application, are you sure?")) {
            var data = {
                port: retrieveValue('port${safeId.apply(protocol)}'),
                useCache: retrieveValue('useCache${safeId.apply(protocol)}')
            };
            console.log(data);
            sendData('/api/protocols/${protocol.getProtocolInstanceId()}', 'POST', JSON.stringify(data), 'application/json', (status, response) => {
                location.reload();
            })
        }
    }
</script>
<div id="ProtocolPanel${safeId.apply(protocol)}" class="panel panel-default">

    <div class="panel-heading" role="tab" id="headingProtocol${safeId.apply(protocol)}">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseProtocol${safeId.apply(protocol)}"
               onclick="toggleAccordion('collapseProtocol${safeId.apply(protocol)}')"
               aria-expanded="true" aria-controls="collapseProtocol${safeId.apply(protocol)}">
                Protocol <b>${protocol.getProtocolInstanceId()}</b> ( ${protocol.getProtocol()} )
            </a>
        </h4>
    </div>
    <div id="collapseProtocol${safeId.apply(protocol)}" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingProtocol${safeId.apply(protocol)}">
        <div class="panel-body">


            <div class="input-group">
                <label for="port${safeId.apply(protocol)}" class="control-label">Listening port</label>
                <div>
                    <input id="port${safeId.apply(protocol)}" type="number" class="form-control"
                           value="${protocol.getPort()}">
                </div>
            </div>
            <br>
            <div class="input-group">
                <div class="checkbox">
                    <label>
                        @if(protocol.isUseCache())
                            <input id="useCache${safeId.apply(protocol)}" type="checkbox" checked>
                        @else
                            <input id="useCache${safeId.apply(protocol)}" type="checkbox">
                        @endif
                        Use internal cache
                    </label>
                </div>
            </div>
            <div class="input-group">
                <button class="btn btn-danger" type="button"
                        onclick="updateData${safeId.apply(protocol)}()">Update settings
                </button>
            </div>
            <br>
            <div class="input-group">
                <button class="btn btn-success" type="button"
                        onclick="loadFromClipboard${safeId.apply(protocol)}()">Load DNS from clipboard
                </button>
                <br>
                <p>At minimum[{"name":"google.com"}] All will be set to the TPM machine (127.0.0.1)</p>
            </div>
            <br>
            <div class="form-inline">
                <div class="form-group">
                    <label class="sr-only" for="newRegistered${safeId.apply(protocol)}">Name</label>
                    <input id="newRegistered${safeId.apply(protocol)}" type="text" size="30"
                           class="form-control">
                </div>
                <div class="form-group">
                    <label class="sr-only" for="newRegisteredIp${safeId.apply(protocol)}">Ip</label>
                    <input id="newRegisteredIp${safeId.apply(protocol)}" type="text" size="30"
                           class="form-control">
                </div>
                <button class="btn btn-default" type="button"
                        onclick="createNewRegistered${safeId.apply(protocol)}()">
                    Create
                </button>
            </div>
            <br>
            <table class="table table-bordered table-striped">
                <tr>
                    <th>Registered DNS</th>
                    <th>Ip</th>
                    <th></th>
                </tr>
                @for(var registered:protocol.getRegistered())
                    <tr>
                        <td>${registered.getName()}</td>
                        <td>${registered.getIp()}</td>
                        <td>
                            <button type="button" class="btn  btn-danger"
                                    onclick="deleteRegistered('${registered.getName()}')">
                                Delete
                            </button>
                        </td>
                    </tr>
                @endfor
            </table>
            <br>
            <div class="form-inline">
                <div class="form-group">
                    <label class="sr-only" for="newBlocked${safeId.apply(protocol)}">Name</label>
                    <input id="newBlocked${safeId.apply(protocol)}" type="text" size="30"
                           class="form-control">
                </div>
                <button class="btn btn-default" type="button"
                        onclick="createNewBlocked${safeId.apply(protocol)}()">
                    Create
                </button>
            </div>
            <br>
            <table class="table table-bordered table-striped">
                <tr>
                    <th>Blocked DNS</th>
                    <th></th>
                </tr>
                @for(var blocked:protocol.getBlocked())
                    <tr>
                        <td>${blocked}</td>
                        <td>
                            <button type="button" class="btn  btn-danger"
                                    onclick="deleteBlocked('${blocked}')">
                                Delete
                            </button>
                        </td>
                    </tr>
                @endfor
            </table>
        </div>
    </div>
</div>
