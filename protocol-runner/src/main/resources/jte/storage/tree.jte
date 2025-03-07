@param org.kendar.ui.dto.FileTreeItemDto data
<li><span class="bi bi-folder2-open" onclick=""></span>${data.getName()}
    <ul>
        @for(var child :data.getChildren())
            @if(child.isDirectory())
                <li id="${child.getSafeIndex()}">
                    <span class="bi bi-folder2"></span>
                    <span
                       hx-get="/storage/tree?parent=${child.getPath()}"
                       hx-trigger="click"
                       hx-target="#${child.getSafeIndex()}"
                    > ${child.getName()}</span>

                </li>
            @else
                <li><span class="bi bi-file-x"></span>${child.getName()}</li>
            @endif
        @endfor
    </ul>
</li>