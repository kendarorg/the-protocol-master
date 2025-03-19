@param org.kendar.plugins.apis.dtos.ReplacerItemFile data

<div id="singleFile">
@if(data!=null)
    <input type="hidden" id="currentFileName" value="${data.getFileName()}">

    <div class="input-group">
        <button type="button" class="btn  btn-danger"
                onclick="deleteContentFile()">
            Delete
        </button>
        <button type="button" class="btn  btn-success"
                onclick="saveContentFile()">
            Update
        </button>
    </div>
    <br>
    <div class="input-group">
        <div class="checkbox">
            <label>
                @if(data.getReplacerItem().isRegex())
                    <input id="regex" type="checkbox" checked> Is regexp
                @else
                    <input id="regex" type="checkbox"> Is regexp
                @endif
            </label>
        </div>
    </div>
    <br>
    <div class="input-group">
        <label for="toFind" class="control-label">To Find</label>
        <div>
            <textarea id="toFind" rows="4" cols="60" class="form-control">${data.getReplacerItem().getToFind()}</textarea>
        </div>
    </div>
    <br>
    <div class="input-group">
        <label for="toReplace" class="control-label">To Replace</label>
        <div>
            <textarea id="toReplace" rows="4" cols="60" class="form-control">${data.getReplacerItem().getToReplace()}</textarea>
        </div>
    </div>
    <br>
@endif
</div>