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
    function updateData${safeId.apply(protocol)}() {
        if (confirm("You will be restarting the application, are you sure?")) {
            var data = {
                port:retrieveValue('port${safeId.apply(protocol)}'),
                useCache:retrieveValue('useCache${safeId.apply(protocol)}')
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
        </div>
    </div>
</div>
