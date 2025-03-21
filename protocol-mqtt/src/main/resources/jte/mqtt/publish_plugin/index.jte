@param org.kendar.ui.dto.SinglePluginDto data

!{
    var globalSettings = org.kendar.di.DiService.getThreadContext().getInstance(org.kendar.settings.GlobalSettings.class);
    var pluginSettings =((org.kendar.mqtt.MqttProtocolSettings)globalSettings.getProtocol(data.getInstanceId(),org.kendar.mqtt.MqttProtocolSettings.class));
}

<div class="panel panel-default">
    <script type="text/javascript">
        function sendQueueData() {
            var c = getSelectedConnection();
            var topic = retrieveValue("topic")

            var path = '/api/protocols/${data.getInstanceId()}/plugins/publish-plugin/connections/' + c + '/' + topic;
            var toSend = retrieveValues({
                contentType: null,
                body: null
            });
            console.log("Sending");
            console.log(toSend);

            sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status) => {
                if (status == 200) {
                    alert("Data Sent!");
                } else {
                    alert("Error sending data!")
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
                <label for="topic" class="control-label">Topic</label>
                <input id="topic" type="text" class="form-control"
                       value="">
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
