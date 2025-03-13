@param org.kendar.ui.dto.GlobalsDto data

<head>

    <title>The Protocol Master - Global Plugins</title>
    @template.head()
</head>
<body>
@template.header("globalpl")
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingAvailableFunctions">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion" href="#collapseAvailableFunctions"
                           aria-expanded="true" aria-controls="collapseAvailableFunctions">
                            Global plugins
                        </a>
                    </h4>
                </div>
                <div id="collapseAvailableFunctions" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingAvailableFunctions">
                    <div class="panel-body">
                        <table class="table">
                            <tr><th>Plugin</th><th>Activation</th><th>Manage</th></tr>
                            @for(var plugin : data.getPlugins())
                                <tr><td>${plugin.getId()}</td><td>
                                        @if(plugin.isActive())
                                            <button type="button" class="btn  btn-danger"
                                                    onclick="getData('/api/global/plugins/${plugin.getId()}/stop','GET',locationReload)">Stop</button>
                                        @else
                                            <button type="button" class="btn btn-success"
                                                    onclick="getData('/api/global/plugins/${plugin.getId()}/start','GET',locationReload)">Start</button>

                                        @endif
                                    </td>
                                    <td>
                                        <a class="btn" href="/plugins/global/${plugin.getId()}">Manage</a>
                                    </td></tr>
                            @endfor
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
@template.footer()

</body>