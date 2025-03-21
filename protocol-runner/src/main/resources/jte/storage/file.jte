@param org.kendar.ui.dto.FileItemDto data
<div id="resultsContainer">
    <form id="fileContainerData">
        <div class="input-group">
            <label for="fileContent" class="control-label">${data.getPath()}</label>
            <div>
                <textarea id="fileContent" type="text" rows="8" cols="200" class="form-control"
                >${data.getContent()}</textarea>
            </div>
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
            &nbsp;
            <button type="button" class="btn  btn-success"
                    onclick="prettifyJson('fileContent')">
                Prettify Json
            </button>
        </div>
    </form>
</div>