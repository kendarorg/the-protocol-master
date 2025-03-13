@param org.kendar.ui.dto.FileTreeItemDto data

@if(data.isOpen())
<li id="${data.getSafePath()}" class="theOpenOne"><span class="bi bi-folder2-open"
                                       onclick="closeTree('#${data.getSafePath()}','${data.getPath()}')">

    </span><span id="${data.getSafePath()}_name" class="selectedClass">${data.getName()}</span>
    <ul class="tree">
        @for(var child :data.getChildren())
            @if(child.isDirectory())
                <li class="theChildDir" id="${child.getSafePath()}" >
                    <span class="bi bi-folder2"
                          onclick="openTree('#${child.getSafePath()}','${child.getPath()}')">
                    </span>
                    <span id="${child.getSafePath()}_name"
                    >${child.getName()}</span>
                </li>
            @else
                <li class="theChildFile" ><span class="bi bi-file-x"></span>${child.getName()}</li>
            @endif
        @endfor
    </ul>
</li>
@else
    <li class="theClosedOne" id="${data.getSafePath()}"><span class="bi bi-folder2"
              onclick="openTree('#${data.getSafePath()}','${data.getPath()}')">

        </span><span class="selectedClass" id="${data.getSafePath()}_name">${data.getName()}</span></li>
@endif