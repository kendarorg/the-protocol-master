@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var pluginSettings =((org.kendar.amqp.v09.AmqpProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.amqp.v09.AmqpProtocolSettings.class));
}

<div class="panel panel-default">
    <script type="text/javascript">
        function sendQueueData() {
            var c = getSelectedConnection().split("_");
            var path = '/api/protocols/${data.getInstanceId()}/plugins/publish-plugin/connections/' + c[0] + '/' + c[1];
            var toSend = retrieveValues(
                {
                    contentType: null,
                    appId: null,
                    propertyFlag: null,
                    deliveryMode: null,
                    deliveryTag: null,
                    body: null,
                    queue:null,
                    exchange:null
                });
            console.log("Sending");
            console.log(toSend);

            sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status) => {
                if (status == 200) {
                    showAlert("Data Sent!");
                }

            })
        }
    </script>
    <div class="panel-heading" role="tab" id="headingSpecificPlugin">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseSpecificPlugin"
               onclick="toggleAccordion('collapseSpecificPlugin')"
               aria-expanded="true" aria-controls="collapseSpecificPlugin">
                Publish messages
            </a>
        </h4>
    </div>
    <div id="collapseSpecificPlugin" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingSpecificPlugin">
        <div class="panel-body">

            <div class="input-group" id="availableConnections" hx-target="this" hx-swap="outerHTML"
                 hx-get="/protocols/${data.getInstanceId()}/plugins/${data.getId()}/connections"
                 hx-trigger="load"></div>
            <div class="input-group">
                <label for="contentType" class="control-label">Content Type</label>
                <select id="contentType" class="form-control">
                    <option value="application/json" selected>Json</option>
                    <option value="application/octet-stream">Binary (Base64 encoded)</option>
                    <option value="text/plain">Text</option>
                </select>
            </div>
            <div class="input-group">
                <label for="appId" class="control-label">App Id</label>
                <input id="appId" type="text" class="form-control"
                       value="">
            </div>

            <div class="input-group">
                <label for="queue" class="control-label">Queue</label>
                <input id="queue" type="text" class="form-control"
                       value="">
            </div>

            <div class="input-group">
                <label for="exchange" class="control-label">Exchange</label>
                <input id="exchange" type="text" class="form-control"
                       value="">
            </div>
            <div class="input-group">
                <label for="propertyFlag" class="control-label">Property Flag</label>
                <input id="propertyFlag" type="text" class="form-control"
                       value="-28664">
            </div>
            <div class="input-group">
                <label for="deliveryMode" class="control-label">Delivery Mode</label>
                <input id="deliveryMode" type="number" class="form-control"
                       value="1">
            </div>
            <div class="input-group">
                <label for="deliveryTag" class="control-label">Delivery Tag</label>
                <input id="deliveryTag" type="number" class="form-control"
                       value="1">
            </div>
            <div class="input-group">
                <label for="body" class="control-label">Message</label>
                <textarea id="body" class="form-control" rows="10" cols="200"></textarea>
                <br>
            </div>
            <div class="input-group">
                <button type="button" class="btn  btn-success"
                        onclick="sendQueueData()">Send
                </button>
            </div>

        </div>
    </div>
</div>
