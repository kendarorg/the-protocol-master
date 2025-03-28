@param org.kendar.ui.dto.RecordingSearchResult data
<table id="recordingSearchResult" class="table table-bordered table-striped">
    <tr>
        @for(var label:data.getFields())
            <th>${label}</th>
        @endfor
    </tr>
    @for(var e : data.getRows())
        <tr>
            @for(var label:data.getFields())

                <td>
                    @if(label.equalsIgnoreCase("index"))
                        <span id="row_index_${data.convert(e.get(label))}" onclick="openById(${data.convert(e.get(label))})">[${data.convert(e.get(label))}]</span>
                    @else
                        ${data.convert(e.get(label))}
                    @endif
                </td>

            @endfor
        </tr>
    @endfor
</table>

