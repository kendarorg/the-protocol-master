@param org.kendar.ui.dto.ProtocolStatusDto data

!{
    var sortedData = data.getWildcardPlugins().stream().
    sorted(java.util.Comparator.comparing(org.kendar.ui.dto.WildcarPluginDto::getId)).toList();
 }

@if(!(boolean) data.getParameters().get("avoidScript"))
    <script type="text/javascript">
        function reloadWildcard(callback) {
            console.log("reloadWildcard")
            var isOpen = isAccordionOpen('collapseWildcard');
            return htmx.ajax('GET', '/plugins/wildcard?avoidScript=true', '#wildcardPanel').then(() => {
                if (isOpen) openAccordion('collapseWildcard');
            })
        }
    </script>
@endif
<div id="wildcardPanel" class="panel panel-default">

    <div class="panel-heading" role="tab" id="headingWildcard">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseWildcard"
               onclick="toggleAccordion('collapseWildcard')"
               aria-expanded="true" aria-controls="collapseWildcard">
                Wildcard plugins handling
            </a>
        </h4>
    </div>
    <div id="collapseWildcard" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingWildcard">
        <div class="panel-body">
            <table class="table">
                <tr>
                    <th>Plugin</th>
                    <th></th>
                    <th></th>
                </tr>
                @for(var element : sortedData)
                    <tr>
                        <td>${element.getId()}</td>
                        <td>
                            @if(element.getActive()==0)
                                <button type="button" class="btn  btn-success"
                                        onclick="getData('/api/protocols/all/plugins/${element.getId()}/start','GET',reloadAllPlugins)">
                                    Start
                                </button>
                            @elseif(element.someActive())
                                <button type="button" class="btn  btn-warning"
                                        onclick="getData('/api/protocols/all/plugins/${element.getId()}/start','GET',reloadAllPlugins)">
                                    Start
                                </button>
                            @endif

                        </td>
                        <td>
                            @if(element.getNotActive()==0)
                                <button type="button" class="btn  btn-danger"
                                        onclick="getData('/api/protocols/all/plugins/${element.getId()}/stop','GET',reloadAllPlugins)">
                                    Stop
                                </button>
                            @elseif(element.someActive())
                                <button type="button" class="btn  btn-warning"
                                        onclick="getData('/api/protocols/all/plugins/${element.getId()}/stop','GET',reloadAllPlugins)">
                                    Stop
                                </button>
                            @endif
                        </td>
                    </tr>
                @endfor
            </table>
        </div>
    </div>
</div>