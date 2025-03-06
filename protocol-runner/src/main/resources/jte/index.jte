@param org.kendar.settings.GlobalSettings data
<head>

    <title>The Protocol Master</title>
    @template.head()
</head>
<body>
@template.header("index")


<script type="application/javascript">
    function terminate() {
        if (confirm("You want to terminate the process?") == true) {
            getData("/api/global/terminate", "GET", () => {
                alert("Application terminated");
            })
        }
    }

    function restart() {
        if (confirm("You want to restart the process?") == true) {
            getData("/api/global/restart", "GET", () => {
                alert("Application restarted");
            })
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
                            TPM Main settings
                        </a>
                    </h4>
                </div>
                <div id="collapseStorage" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingStorage">
                    <div class="panel-body">

                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-danger" onclick="terminate()">
                                Terminate
                            </a>
                        </div>
                        <br>
                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-danger" onclick="restart()">
                                Restart
                            </a>
                        </div>
                        <br>
                        <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                    >API Port</button>
                                </span>
                            <input type="text" readonly="true"
                                   class="form-control" value="${data.getApiPort()}">
                        </div>
                        <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                    >Data Dir</button>
                                </span>
                            <input type="text" readonly="true"
                                   class="form-control" value="${data.getDataDir()}">
                        </div>
                        <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                    >Plugins Dir</button>
                                </span>
                            <input type="text" readonly="true"
                                   class="form-control" value="${data.getPluginsDir()}">
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
@template.footer()

</body>