@param org.kendar.ui.dto.SinglePluginDto data

<div class="panel panel-default">
    <script type="text/javascript">
        function sendQueueData() {
            var c = getSelectedConnection();
            var path = '/api/protocols/${data.getInstanceId()}/plugins/publish-plugin/connections/' + c;
            var toSend = retrieveValues(
                {
                    contentType: null,
                    topic: null,
                    partition: 0,
                    key: null,
                    acks: 1,
                    body: null
                });
            console.log("Producing");
            console.log(toSend);

            sendData(path, 'POST', JSON.stringify(toSend), 'application/json', (status) => {
                if (status == 200) {
                    showAlert("Message produced!");
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
                    <option value="text/plain" selected>Text</option>
                    <option value="application/json">Json</option>
                    <option value="application/octet-stream">Binary (Base64 encoded)</option>
                </select>
            </div>
            <div class="input-group">
                <label for="topic" class="control-label">Topic</label>
                <input id="topic" type="text" class="form-control" value="">
            </div>
            <div class="input-group">
                <label for="partition" class="control-label">Partition</label>
                <input id="partition" type="number" class="form-control" value="0">
            </div>
            <div class="input-group">
                <label for="key" class="control-label">Key (optional)</label>
                <input id="key" type="text" class="form-control" value="">
            </div>
            <div class="input-group">
                <label for="acks" class="control-label">Acks</label>
                <select id="acks" class="form-control">
                    <option value="1" selected>1 (leader)</option>
                    <option value="-1">-1 (all)</option>
                    <option value="0">0 (none)</option>
                </select>
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
