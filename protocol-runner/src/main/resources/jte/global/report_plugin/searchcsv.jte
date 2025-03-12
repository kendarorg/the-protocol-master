
@param org.kendar.plugins.apis.GlobalReportResult data
${String.join("\t",data.getFields())}
@for(var e : data.getRows())
${String.join("\t",data.getFields().stream().map(f->data.convert(e.get(f)))))
    <tr>
        @for(var label:data.getFields())
            <td>${data.convert(e.get(label))}</td>
        @endfor
    </tr>
@endfor

