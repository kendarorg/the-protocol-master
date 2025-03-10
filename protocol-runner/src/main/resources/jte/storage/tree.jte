@param org.kendar.ui.dto.FileTreeItemDto data

@if(data.isOpen())
<li  id="${data.getSafePath()}"><span class="bi bi-folder2-open"
                                       onclick="closeTree('#${data.getSafePath()}','${data.getPath()}')"></span>${data.getName()}
    <ul class="tree">
        @for(var child :data.getChildren())
            @if(child.isDirectory())
                <li  id="${child.getSafePath()}" >
                    <span class="bi bi-folder2"
                          onclick="openTree('#${child.getSafePath()}','${child.getPath()}').then(()=>openFiles('${child.getPath()}'))">
                    </span>
                    <span
                    > ${child.getName()}</span>

                </li>
            @else
                <li ><span class="bi bi-file-x"></span>${child.getName()}</li>
            @endif
        @endfor
    </ul>
</li>
@else
    <li  id="${data.getSafePath()}"><span class="bi bi-folder2"
              onclick="openTree('#${data.getSafePath()}','${data.getPath()}').then(()=>openFiles('${data.getPath()}'))">

        </span>&nbsp;${data.getName()}</li>
@endif