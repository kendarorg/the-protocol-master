@param java.util.List<String> files

<table id="filesList" class="table table-bordered table-striped">
    <tr>
        <th>Replace Id</th>
    </tr>
    @for(var file:files)
        <tr>
            <td><span onclick="openSpecificFile('${file}')">${file}</span></td>
        </tr>
    @endfor
</table>
