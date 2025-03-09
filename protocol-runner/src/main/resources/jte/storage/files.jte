@param org.kendar.ui.dto.FileTreeItemDto data
<div id="resultsContainer">
<ul  style="list-style-type: none;">
    @for(var file:data.getChildren())
        @if(!file.isDirectory())
        <li><span onclick="openFile('${file.getPath()}','${file.getName()}')">${file.getName()}</span></li>
        @endif
    @endfor
</ul>
</div>
