<head>

    <title>The Protocol Master - Storage</title>
    @template.head()
</head>
<body>
@template.header("storage")

<script type="text/javascript">
    function cleanStorage() {
        if (confirm("You want to remove ALL data?") == true) {
            getData("/api/global/storage", "DELETE", () => {
                alert("All data deleted");
            })
        }
    }

    var selectedElement=null;
    var selectedPath=null;
    function toggle(newElement){
        newElement+="_name";
        if(selectedElement!=null && selectedElement!=newElement){
            htmx.find(selectedElement).classList.toggle("selectedClass")
            selectedElement = newElement;
            htmx.find(newElement).classList.toggle("selectedClass")
        }else if(selectedElement ==null){
            selectedElement = newElement;
            htmx.find(newElement).classList.toggle("selectedClass")
        }

    }
    function openTree(target,path) {
        selectedPath=path;
        htmx.ajax('GET', '/storage/tree?parent='+path, {target:target, swap:'outerHTML'}).then(()=>openFiles(path)).then(()=>toggle(target));
    }
    function openFiles(path) {
        selectedPath=path;
        htmx.ajax('GET', '/storage/files?parent='+path, "#resultsContainer");
    }
    function openFile(path,name) {
        htmx.ajax('GET', '/storage/file?parent='+path+'&name='+name, "#resultsContainer");
    }

    function closeTree(target,path) {
        selectedPath=path;
        htmx.ajax('GET', '/storage/tree?close=true&parent='+path, {target:target, swap:'outerHTML'}).then(()=>openFiles(path)).then(()=>toggle(target));
    }

    function saveFile(path,data,callback) {
        return sendData('/storage/file?parent='+path, 'POST', data, 'application/json',callback)
    }
    function deleteFile(path) {
        if (confirm("You want to remove "+path+"?") == true) {
            return getData('/storage/file?parent=' + path, 'delete', 'application/json')
        }
    }


</script>
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <!--
            HANDLE -FULL- STORAGE
            -->

            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingStorage">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseStorage"
                           onclick="toggleAccordion('collapseStorage')"
                           aria-expanded="true" aria-controls="collapseStorage">
                            Global Storage-Scenario Handling
                        </a>
                    </h4>
                </div>
                <div id="collapseStorage" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingStorage">
                    <div class="panel-body">

                        <div class="input-group">
                            <a href="#" type="button" class="btn  btn-success"
                               onclick="downloadURI('/api/global/storage')">
                                Download
                            </a>
                        </div>
                        <br>
                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-danger" onclick="cleanStorage()">
                                Clean All
                            </a>
                        </div>
                        <br>
                        <form enctype="multipart/form-data">

                            <div class="input-group">
                                <span class="input-group-btn">
                                    <label for="uploadStorage" class="btn btn-default"
                                    >Browse</label>
                                    <button class="btn btn-default" type="button"
                                            onclick="async_file_send()">Upload</button>
                                </span>
                                <input id="uploadStorageFile" type="text" readonly="true" size="30"
                                       class="form-control">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                            onclick="async_file_clean('uploadStorage')">Clean</button>
                                </span>
                            </div>
                            <div><input id="uploadStorage" class="btn btn-success"
                                        style="visibility:hidden;height: 0px;width: 0px;padding: 0px;"
                                        size="0"
                                        contentType="application/octet-stream" binary="true" type="file"
                                        path="api/global/storage"></div>
                        </form>
                    </div>
                </div>
            </div>
            <!-- 
            BROWSE STORAGE
            -->
            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingBrowse">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseBrowse"
                           onclick="toggleAccordion('collapseBrowse')"
                           aria-expanded="true" aria-controls="collapseBrowse">
                            Browse Storage
                        </a>
                    </h4>
                </div>
                <div id="collapseBrowse" class="panel-collapse collapse" role="tabpanel"
                     aria-labelledby="headingBrowse">

                    <div class="panel-body">
                        <div class="container-fluid">
                            <div class="col-xs-4" >
                                <ul class="tree" style="list-style-type: none;">
                                    <li hx-swap="outerHTML" hx-get="/storage/tree?parent=" hx-trigger="load"></li>
                                </ul>
                            </div>

                            <div class="col-xs-8">
                                <div id="resultsContainer" hx-get="/storage/files?parent=" hx-trigger="load">

                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
<script type="text/javascript">
    document.getElementById('uploadStorage').addEventListener('change', (evt) => handle_file_select(evt), false);
</script>
@template.footer()

</body>