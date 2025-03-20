@param org.kendar.plugins.apis.dtos.MockStorageFile data


<div id="singleFile">
@if(data!=null)
    !{
    var mapper = new org.kendar.utils.JsonMapper();
    var input = mapper.serialize(data.getMockStorage().getInput());
    var output = mapper.serialize(data.getMockStorage().getOutput());
    if(data.getMockStorage().getInput()==null)input="";
    if(data.getMockStorage().getOutput()==null)output="";
}

    <div class="input-group">
        <button type="button" class="btn  btn-danger"
                onclick="deleteContentFile()">
            Delete
        </button>
        &nbsp;
        <button type="button" class="btn  btn-success"
                onclick="saveContentFile()">
            Update
        </button>
    </div>
    <br>


    <div class="input-group ">
        <label for="fileName" class="control-label">File Name</label>
        <div>
            <input id="fileName" type="text" readonly="readonly" class="form-control" value="${data.getFileName()}"/>
        </div>
    </div>
    <br>
    <div class="input-group">
        <label for="count" class="control-label">Count</label>
        <input id="count" type="number" class="form-control"
               value="${data.getMockStorage().getCount()}">
    </div>
    <br>
    <div class="input-group">
        <label for="nthRequest" class="control-label">Nth request</label>
        <input id="nthRequest" type="number" class="form-control"
               value="${data.getMockStorage().getNthRequest()}">
    </div>

    <br>
    <div class="panel-body">
        <div class="col-xs-4 ">

            <div class="input-group ">
                <label for="index" class="control-label">Index-Sets the order of mocks if needed</label>
                <div>
                    <input id="index" type="text" class="form-control" value="${data.getMockStorage().getIndex()}"/>
                </div>
            </div>
            <div class="input-group ">
                <label for="type" class="control-label">Type</label>
                <div>
                    <input id="type" type="text" class="form-control" value="${data.getMockStorage().getType()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="caller" class="control-label">Caller</label>
                <div>
                    <input id="caller" type="text" class="form-control" value="${data.getMockStorage().getCaller()}"/>
                </div>
            </div>
        </div>
        <div class="col-xs-4 ">

            <div class="input-group ">
                <label for="inputType" class="control-label">Input Type</label>
                <div>
                    <input id="inputType" type="text" class="form-control" value="${data.getMockStorage().getInputType()}"/>
                </div>
            </div>

            <div class="input-group ">
                <label for="outputType" class="control-label">Output Type</label>
                <div>
                    <input id="outputType" type="text" class="form-control" value="${data.getMockStorage().getOutputType()}"/>
                </div>
            </div>
        </div>
        <div class="col-xs-4 ">
            <div class="input-group ">
                <button type="button" class="btn  btn-success"
                        onclick="pasteData()">
                    Paste data
                </button>
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
    </div>
@endif
</div>