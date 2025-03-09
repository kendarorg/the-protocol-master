@param org.kendar.ui.dto.FileItemDto data
<div id="resultsContainer">
    Path <input type="text" value="${data.getPath()}"/><br/>
    Name <input type="text" value="${data.getName()}"/><br/>
    Content
    <textarea>${data.getContent()}</textarea>
</div>