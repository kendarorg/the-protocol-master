@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var ssl =((org.kendar.http.HttpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.http.HttpProtocolSettings.class)).getSSL();
}
<div class="panel-body" id="sslHostSettings">
    <script>
        function addHost(){
            var val = retrieveValue('sslHostToAdd').;
            if(val!==null && val.length>0){
                sendData('/api/protocols/${data.getInstanceId()}/plugins/ssl-plugin/hosts?host='+val,'POST',null,'application/json',()=>{
                    htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/ssl-plugin/hosts', {
                        target:'#sslHostSettings', swap:'outerHTML'
                    })
                })
            }
        }
        function removeHost(host){
            if(host!==null && host.length>0){
                sendData('/api/protocols/${data.getInstanceId()}/plugins/ssl-plugin/hosts?host='+host,'DELETE',null,'application/json',()=>{
                    htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/ssl-plugin/hosts', {
                        target:'#sslHostSettings', swap:'outerHTML'
                    })
                })
            }
        }
    </script>
    <div class="input-group">
                <span class="input-group-btn">
                    <button class="btn btn-default" type="button"
                            onclick="addHost()">
                        Add Host</button>
                </span>
        <input id="sslHostToAdd" type="text" size="30"
               class="form-control">
    </div>
    <br>
    <table class="table table-bordered table-striped">
        <tr>
            <th>Hosts</th>
            <th></th>
        </tr>
        @for(var element : ssl.getHosts())
            <tr>
                <td>${element}</td>
                <td>
                    <button class="btn btn-danger" type="button"
                            onclick="removeHost('${element}')">Remove
                    </button>
                </td>
            </tr>
        @endfor
    </table>
</div>