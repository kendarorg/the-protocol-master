@param org.kendar.amqp.v09.plugins.apis.dtos.AmqpConnections data
<div id="availableConnections">
    <div class="input-group">
        <script type="text/javascript">
            function reloadConnections() {
                htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/publish-plugin/connections', {
                    target: '#availableConnections',
                    swap: 'outerHTML'
                })
            }
            function getSelectedConnection(){
                var ele = document.getElementsByName('connectionRadios');
                for (i = 0; i < ele.length; i++) {
                    if (ele[i].checked)
                        return ele[i].value;
                }
                return "0_0";
            }
        </script>
        <label for="connectionChoice" class="control-label">
            Select the connection
        </label>
        &nbsp;
        <button type="button" class="btn  btn-success"
                onclick="reloadConnections()">Reload
        </button>
        <div id="connectionChoice">
            @for(var i=0;i<data.getConnections().size();i++)
                <div class="radio">
                    <label>
                        <input type="radio" name="connectionRadios"
                               id="connectionRadios${i}"
                               value='${data.getConnections().get(i).getId()+"_"+data.getConnections().get(i).getChannel()}'>
                        ${data.getConnections().get(i).toString()}
                    </label>
                </div>
            @endfor
        </div>
    </div>

</div>