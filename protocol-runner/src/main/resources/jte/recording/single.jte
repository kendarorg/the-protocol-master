@param org.kendar.ui.dto.RecordingLine data

!{
    var mapper = new org.kendar.utils.JsonMapper();
    var input = mapper.serialize(data.getData().getInput());

    var output = mapper.serialize(data.getData().getOutput());
    if(data.getData().getInput()==null)input="";
    if(data.getData().getOutput()==null)output="";
}

<div id="recordingSingleFile" class="panel panel-default">
    <div class="panel-body">
        <div class="col-xs-4 ">
            <div class="input-group ">
                <label for="index" class="control-label">Index</label>
                <div>
                    <input id="index" type="number" class="form-control" value="${data.getData().getIndex()}"/>
                </div>
            </div>


            <div class="input-group ">
                <label for="type" class="control-label">Type</label>
                <div>
                    <input id="type" type="text" class="form-control" value="${data.getData().getType()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="caller" class="control-label">Caller</label>
                <div>
                    <input id="caller" type="text" class="form-control" value="${data.getData().getCaller()}"/>
                </div>
            </div>
        </div>
        <div class="col-xs-4 ">
            <div class="input-group ">
                <label for="connectionId" class="control-label">Connection Id</label>
                <div>
                    <input id="connectionId" type="number" class="form-control"
                           value="${data.getData().getConnectionId()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="inputType" class="control-label">Input Type</label>
                <div>
                    <input id="inputType" type="text" class="form-control" value="${data.getData().getInputType()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="outputType" class="control-label">Output Type</label>
                <div>
                    <input id="outputType" type="text" class="form-control" value="${data.getData().getOutputType()}"/>
                </div>
            </div>
        </div>
        <div class="col-xs-4 ">

            <div class="input-group ">
                <label for="durationMs" class="control-label">Duration MS</label>
                <div>
                    <input id="durationMs" type="text" class="form-control" value="${data.getData().getDurationMs()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="timestamp" class="control-label">Timestamp</label>
                <div>
                    <input id="timestamp" type="text" class="form-control" value="${data.getData().getTimestamp()}"/>
                </div>
            </div>
        </div>

        <div class="col-xs-12 ">
            <br>
            <div class="input-group ">
                <label for="input" class="control-label">Input</label>
                &nbsp;
                <button type="button" class="btn  btn-success"
                        onclick="prettifyJson('input')">
                    Prettify Json
                </button>
                <br>
                <div>
                    <textarea id="input" type="text" cols="200" rows="20"
                              class="form-control">${input}</textarea>
                </div>
            </div>
        </div>

        <div class="col-xs-12 ">
            <br>
            <div class="input-group ">
                <label for="output" class="control-label">Output</label>
                &nbsp;
                <button type="button" class="btn  btn-success"
                        onclick="prettifyJson('output')">
                    Prettify Json
                </button>
                <br>
                <div>
                    <textarea id="output" type="text" cols="200" rows="20"
                              class="form-control">${output}</textarea>
                </div>
            </div>
        </div>
        <div class="col-xs-12 "><label for="tags" class="control-label">Tags</label>
            <br>
            <table id="tags" class="table table-bordered table-striped">
                <tr><th>Id</th><th>Value</th></tr>
                @for(var kvp:data.getIndex().getTags().entrySet())
                <tr><td>${kvp.getKey()}</td><td>${kvp.getValue()}</td></tr>
                @endfor
            </table>
        </div>
    </div>
</div>