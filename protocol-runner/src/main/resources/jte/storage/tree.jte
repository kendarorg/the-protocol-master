@param org.kendar.ui.dto.FileTreeItemDto data
<div>
    <ul class="tree-padding tree-vertical-lines tree-horizontal-lines tree-summaries tree-markers tree-buttons">
    @for(var element : data.getChildren())
        @if(!element.isDirectory())
            <li>${element.getName()}</li>
        @else
            <li>
                <details>
                    <summary>${element.getName()}</summary>
                    <ul>
                        @for(var plElement:element.getChildren())

                            <details>
                                <summary>${plElement.getName()}</summary>
                                <ul>
                                    @for(var pflElement:plElement.getChildren())
                                        <li>${pflElement.getName()}</li>
                                    @endfor
                                </ul>
                            </details>

                        @endfor
                    </ul>
                </details>
            </li>
        @endif
    @endfor
</ul>
</div>