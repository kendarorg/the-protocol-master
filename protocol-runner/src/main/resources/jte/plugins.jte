@param org.kendar.ui.dto.ProtocolStatusDto data

<head>

    <title>The Protocol Master - Plugins</title>
    @template.head()
</head>
<body>
@template.header("plugins")
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <!--
            ACTIVE PLUGINS
            -->
            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingAvailableFunctions">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseAvailableFunctions"
                           aria-expanded="true" aria-controls="collapseAvailableFunctions">
                            Active plugins
                        </a>
                    </h4>
                </div>
                <div id="collapseAvailableFunctions" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingAvailableFunctions">
                    <div class="panel-body">
                        <table class="table">
                            <tr>
                                <th>Instance</th>
                                <th>Plugin</th>
                                <th>Stop</th>
                            </tr>
                            @for(var element : data.getActivePlugins())
                                <tr>
                                    <td>${element.getInstanceId()}</td>
                                    <td>${element.getId()}</td>
                                    <td>
                                        <button type="button" class="btn  btn-danger"
                                                onclick="getData('/api/protocols/${element.getInstanceId()}/plugins/${element.getId()}/stop','GET',()=>location.reload())">
                                            Stop
                                        </button>
                                    </td>
                                </tr>
                            @endfor
                        </table>
                    </div>
                </div>
            </div>

            <!--
            WILDCARD PLUGIN ACTIVATION PLUGINS
            -->
            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingWildcardHandling">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseWildcardHandling"
                           aria-expanded="true" aria-controls="collapseWildcardHandling">
                            Wildcard plugins handling
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

            <!--
            PROTOCOL PLUGINS
            -->
            @for(var element : data.getProtocols())
                <div class="panel panel-default">
                    <div class="panel-heading" role="tab" id="heading${element.getInstanceId()}">
                        <h4 class="panel-title">
                            <a class="collapsed" role="button" data-toggle="collapse" data-parent="#accordion"
                               href="#collapse${element.getInstanceId()}" aria-expanded="false"
                               aria-controls="collapse${element.getInstanceId()}">
                                Protocol <b>${element.getInstanceId()}</b> ( ${element.getProtocol()} )
                            </a>
                        </h4>
                    </div>
                    <div id="collapse${element.getInstanceId()}" class="panel-collapse collapse" role="tabpanel"
                         aria-labelledby="heading${element.getInstanceId()}">
                        <div class="panel-body">
                            <table class="table">
                                <tr>
                                    <th>Instance</th>
                                    <th>Plugin</th>
                                    <th>Status</th>
                                    <th>Manage</th>
                                </tr>
                                @for(var plugin : element.getPlugins())
                                    <tr>
                                        <td>${plugin.getInstanceId()}</td>
                                        <td>${plugin.getId()}</td>
                                        <td>
                                            @if(plugin.isActive())
                                                <button type="button" class="btn  btn-danger"
                                                        onclick="getData('/api/protocols/${plugin.getInstanceId()}/plugins/${plugin.getId()}/stop','GET',()=>location.reload())">
                                                    Stop
                                                </button>
                                            @else
                                                <button type="button" class="btn btn-success"
                                                        onclick="getData('/api/protocols/${plugin.getInstanceId()}/plugins/${plugin.getId()}/start','GET',()=>location.reload())">
                                                    Start
                                                </button>

                                            @endif
                                        </td>
                                        <td>
                                            <a class="btn" href="/plugins/${plugin.getInstanceId()}/${plugin.getId()}">Manage</a>
                                        </td>
                                    </tr>
                                @endfor
                            </table>
                        </div>
                    </div>
                </div>

            @endfor
        </div>
    </div>
</div>
@template.footer()

</body>