@param org.kendar.ui.dto.FileTreeItemDto data
<div id="resultsContainer">
    <div class="input-group">
        <span class="input-group-btn">
            <button class="btn btn-default" type="button"
                    onclick="saveFile('${data.getPath()}/'+document.getElementById('createNewStorageFile').value,'',()=>openFiles('${data.getPath()}'))">
                Create</button>
        </span>
        <input id="createNewStorageFile" type="text" size="30"
               class="form-control">
    </div>
    <br>
    <ul style="list-style-type: none;">
        @for(var file:data.getChildren())
            @if(!file.isDirectory())
                <li><span onclick="openFile('${file.getPath()}','${file.getName()}')">${file.getName()}</span></li>
            @endif
        @endfor
    </ul>
</div>
