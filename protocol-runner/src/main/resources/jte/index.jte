@param org.kendar.settings.GlobalSettings data
<head>

    <title>The Protocol Master</title>
    @template.head()
</head>
<body>
@template.header("index")


<script type="text/javascript">
    function cleanAccordionStatus() {
        localStorage.setItem("accordionStatus", "{}");
    }

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
                <div class="panel-heading" role="tab" id="headingMainIndex">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseMainIndex"
                           onclick="toggleAccordion('collapseMainIndex')"
                           aria-expanded="true" aria-controls="collapseMainIndex">
                            TPM Main settings
                        </a>
                    </h4>
                </div>
                <div id="collapseMainIndex" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingMainIndex">
                    <div class="panel-body">

                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-danger" onclick="terminate()">
                                Terminate
                            </a>
                        </div>
                        <br>
                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-warning" onclick="restart()">
                                Restart
                            </a>
                        </div>
                        <br>
                        <div class="input-group">
                            <a href="#" type="button" class="btn btn-success" onclick="cleanAccordionStatus()">
                                Clean open accordions status
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
                        <br>
                        <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                    >Data Dir</button>
                                </span>
                            <input type="text" readonly="true"
                                   class="form-control" value="${data.getDataDir()}">
                        </div>
                        <br>
                        <div class="input-group">
                                <span class="input-group-btn">
                                    <button class="btn btn-default" type="button"
                                    >Plugins Dir</button>
                                </span>
                            <input type="text" readonly="true"
                                   class="form-control" value="${data.getPluginsDir()}">
                        </div>
                        <br>
                        <div class="input-group">
                            <a href="#" type="button" class="btn  btn-success"
                               onclick="downloadURI('/api/global/settings')">
                                Download settings
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>
</div>
@template.footer()

</body>