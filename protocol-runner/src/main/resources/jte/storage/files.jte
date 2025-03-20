@param org.kendar.ui.dto.FileTreeItemDto data
<div id="resultsContainer">
    <div class="input-group">
        <span class="input-group-btn">
            <button class="btn btn-default" type="button"
                    onclick="saveFile('${data.getPath()}/'+retrieveValue('createNewStorageFile'),'',()=>openFiles('${data.getPath()}'))">
                Create</button>
        </span>
        <input id="createNewStorageFile" type="text" size="30"
               class="form-control">
    </div>
    <br>
    <table class="table table-bordered table-striped">
        <tr>
            <th>File</th>
            <th></th>
        </tr>
        @for(var file:data.getChildren())
            @if(!file.isDirectory())
                <tr>
                    <td><span onclick="openFile('${file.getPath()}','${file.getName()}')">${file.getName()}</span></td>
                    <td>
                        <button type="button" class="btn btn-danger"
                                onclick="deleteFile('${file.getPath()}')">
                            Delete
                        </button>
                    </td>
                </tr>
            @endif
        @endfor
    </table>
</div>
