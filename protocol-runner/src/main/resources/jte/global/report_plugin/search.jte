@param org.kendar.plugins.apis.GlobalReportResult data

!{
    var fields = data.getFields();
}

<input type="hidden" id="fieldsCount" value="${fields.size()}">
<input type="hidden" id="rowsCount" value="${data.getRows().size()}">
<table id="reportSearchResult" class="table table-bordered table-striped">
    <tr>
        <th></th>
        @for(var i=0;i<fields.size();i++)
            <th>${fields.get(i)}<input type="hidden" id="label_${i}" value="${fields.get(i)}"></th>
        @endfor
    </tr>
    @for(var i=0;i<data.getRows().size();i++)
        !{
            var e = data.getRows().get(i);
        }
        <tr>
            <td>
                <input id="row_${i}" type="checkbox">
            </td>
            @for(var j=0;j<fields.size();j++)
                !{
                    var f= fields.get(j);
                }
                <td>${data.convert(e.get(f))}<input type="hidden" id="field_${j}_${i}"
                                                    value="${data.convert(e.get(f))}"></td>
            @endfor
        </tr>
    @endfor
</table>

