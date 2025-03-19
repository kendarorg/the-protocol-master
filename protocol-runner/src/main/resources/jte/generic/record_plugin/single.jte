@param org.kendar.plugins.apis.dtos.RecordItemFile data

<div id="singleFile">
@if(data!=null)
    <input type="hidden" id="currentFileName" value="${data.getFileName()}">

    <div class="input-group">
        <button type="button" class="btn  btn-danger"
                onclick="deleteContentFile()">
            Delete
        </button>
        &nbsp;&nbsp;
        <button type="button" class="btn  btn-success"
                onclick="saveContentFile()">
            Update
        </button>
        &nbsp;
        <button type="button" class="btn  btn-success"
                onclick="prettifyJson('body')">
            Prettify Json
        </button>
    </div>
    <br>
    <br>
    <br>
    <div class="input-group">
        <label for="body" class="control-label">Content</label>
        <div>
            <textarea id="body" rows="4" cols="60" class="form-control">${data.getBody()}</textarea>
        </div>
    </div>
    <br>
@endif
</div>