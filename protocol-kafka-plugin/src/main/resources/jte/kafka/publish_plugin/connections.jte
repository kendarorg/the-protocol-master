@param org.kendar.kafka.plugins.apis.dtos.KafkaConnections data
<div id="availableConnections">
    <div class="input-group">
        <script type="text/javascript">
            function reloadConnections() {
                htmx.ajax('GET', '/protocols/${data.getInstanceId()}/plugins/publish-plugin/connections', {
                    target: '#availableConnections',
                    swap: 'outerHTML'
                })
            }

            function getSelectedConnection() {
                var ele = document.getElementsByName('connectionRadios');
                for (i = 0; i < ele.length; i++) {
                    if (ele[i].checked)
                        return ele[i].value;
                }
                return "0";
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
            <div class="radio">
                <label>
                    <input type="radio" name="connectionRadios"
                           id="connectionRadios0"
                           value='0' checked>
                    First available
                </label>
            </div>
            @for(var i=0;i<data.getConnections().size();i++)
                <div class="radio">
                    <label>
                        <input type="radio" name="connectionRadios"
                               id="connectionRadios${i+1}"
                               value='${data.getConnections().get(i).getId()}'>
                        connection ${data.getConnections().get(i).getId()}
                        @if(data.getConnections().get(i).isCanPublish()) (ready) @endif
                    </label>
                </div>
            @endfor
        </div>
    </div>

</div>
