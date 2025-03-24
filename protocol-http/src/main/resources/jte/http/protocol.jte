@param org.kendar.settings.ProtocolSettings protocolSettings

!{
    var protocol = (org.kendar.http.HttpProtocolSettings)protocolSettings;

    var safeId = new java.util.function.Function<org.kendar.settings.ProtocolSettings,String>(){
            @Override
            public String apply(org.kendar.settings.ProtocolSettings protocolSettings) {
                return protocolSettings.getProtocolInstanceId().replaceAll("-","");
            }
        };
}
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
                <label for="http" class="control-label">Http port</label>
                <div>
                    <input id="http" type="number" class="form-control"
                           value="${protocol.getHttp()}">
                </div>
            </div>
            <br>

            <div class="input-group">
                <label for="https" class="control-label">Https port</label>
                <div>
                    <input id="https" type="number" class="form-control"
                           value="${protocol.getHttps()}">
                </div>
            </div>
            <br>

            <div class="input-group">
                <label for="proxy" class="control-label">Proxy port</label>
                <div>
                    <input id="proxy" type="number" class="form-control"
                           value="${protocol.getProxy()}">
                </div>
            </div>
            <br>
        </div>
    </div>
</div>