@param org.kendar.ui.dto.FileItemDto data
<div id="fileContainer" style="max-width: 100%;" >
    <form id="fileContainerData" style="max-width: 100%;">
        <div class="input-group" style="max-width: 100%;">
            <label for="fileContent" class="control-label">${data.getPath()}</label>
                <textarea id="fileContent" type="text" rows="8" style="max-width: 100%;" class="form-control"
                          >${data.getContent()}</textarea>
        </div>
        <br>
        <div class="input-group">
            <button type="button" class="btn btn-success"
                    onclick="saveFile('${data.getPath()}',retrieveValue('fileContent'),()=>openFile('${data.getPath()}'))">
                Update
            </button>
            &nbsp;
            <button type="button" class="btn btn-danger"
                    onclick="deleteFile('${data.getPath()}')">
                Delete
            </button>
        </div>
    </form>
</div>