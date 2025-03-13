
@param org.kendar.plugins.apis.GlobalReportResult data
<html>
<body>
<style>
    td
    {border-left:1px solid black;
        border-top:1px solid black;}
    table
    {border-right:1px solid black;
        border-bottom:1px solid black;}
</style>
<table >
    <tr>
        @for(var label:data.getFields())
            <th style="border:1px;">${label}</th>
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
</body>

</html>

