<head>

    <title>The Protocol Master - Storage</title>
    @template.head()
</head>
<body>
@template.header("storage")

<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <!--
            HANDLE -FULL- STORAGE
            -->
            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingAvailableFunctions">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseAvailableFunctions"
                           aria-expanded="true" aria-controls="collapseAvailableFunctions">
                            Global Storage-Scenario Handling
                        </a>
                    </h4>
                </div>
                <div id="collapseAvailableFunctions" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingAvailableFunctions">
                    <div class="panel-body">
                    </div>
                </div>
            </div>
        </div>
        <!--
        STORAGE INDEX
        -->
        <div class="panel panel-default">
            <div class="panel-heading" role="tab" id="headingWildcardHandling">
                <h4 class="panel-title">
                    <a role="button" data-toggle="collapse" data-parent="#accordion"
                       href="#collapseWildcardHandling"
                       aria-expanded="true" aria-controls="collapseWildcardHandling">
                        Browse Storage
                    </a>
                </h4>
            </div>
            <div id="collapseWildcardHandling" class="panel-collapse collapse" role="tabpanel"
                 aria-labelledby="headingWildcardHandling">
                <div class="panel-body">
                    <table class="table">
                        <tr>
                            <th>Plugin</th>
                            <th></th>
                            <th></th>
                        </tr>
                        @for(var element : data.getPluginGroups())
                            <tr>
                                <td>${element}</td>
                                <td>
                                    <button type="button" class="btn  btn-success"
                                            onclick="getData('/api/protocols/all/plugins/${element}/start','GET',()=>location.reload())">
                                        Start
                                    </button>
                                </td>
                                <td>
                                    <button type="button" class="btn  btn-danger"
                                            onclick="getData('/api/protocols/all/plugins/${element}/stop','GET',()=>location.reload())">
                                        Stop
                                    </button>
                                </td>
                            </tr>
                        @endfor
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>
@template.footer()

</body>