@param org.kendar.ui.dto.FileItemDto data
<div id="resultsContainer" style="max-width: 100%;" >
    <form >
        <div class="input-group">
            <label for="fileContent" class="control-label">${data.getPath()}</label>
                <textarea id="fileContent" type="text" rows="8" style="max-width: 100%;" class="form-control"
                          >${data.getContent()}</textarea>
        </div>
        <br>
        <div class="input-group">
            <button type="button" class="btn btn-success"
                    onclick="saveFile('${data.getPath()}',document.getElementById('fileContent').value)">
                Update/Create
            </button>
        </div>
    </form>
</div>