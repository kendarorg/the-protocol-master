
@param org.kendar.plugins.apis.GlobalReportResult data
<table id="reportSearchResult" class="table table-bordered table-striped">
    <tr>
        @for(var label:data.getFields())
            <th>${label}</th>
        @endfor
    </tr>
    @for(var e : data.getRows())
        <tr>
            @for(var label:data.getFields())
                <td>${data.convert(e.get(label))}</td>
            @endfor
        </tr>
    @endfor
</table>

