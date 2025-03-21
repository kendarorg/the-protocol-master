@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var ssl =((org.kendar.http.HttpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.http.HttpProtocolSettings.class)).getSSL();
}

<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingSpecificPlugin">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSpecificPlugin"
               onclick="toggleAccordion('collapseSpecificPlugin')"
               aria-expanded="true" aria-controls="collapseSpecificPlugin">
                SSL Certificates
            </a>
        </h4>
    </div>
    <div id="collapseSpecificPlugin" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSpecificPlugin">
        <div class="panel-body">
            <div class="input-group">
                <a href="#" type="button" class="btn  btn-success"
                   onclick="downloadURI('/api/protocols/http-01/plugins/ssl-plugin/der')">
                    Download Root Certificate
                </a>
                &nbsp;
                <a href="#" type="button" class="btn  btn-success"
                   onclick="downloadURI('/api/protocols/http-01/plugins/ssl-plugin/key')">
                    Download Private Key
                </a>
            </div>
            <br>
            <div class="input-group">
                <label for="cName" class="control-label">C-Name</label>
                <div>
                    <input id="cName" type="text" readonly="true" size="80" class="form-control"
                           value="${ssl.getCname()}">
                </div>
            </div>
        </div>
    </div>
</div>


<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingSslHosts">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSslHosts"
               onclick="toggleAccordion('collapseSslHosts')"
               aria-expanded="true" aria-controls="collapseSslHosts">
                SSL Hosts
            </a>
        </h4>
    </div>
    <div id="collapseSslHosts" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSslHosts">

        @template.http.ssl_plugin.hosts(data)

    </div>
</div>