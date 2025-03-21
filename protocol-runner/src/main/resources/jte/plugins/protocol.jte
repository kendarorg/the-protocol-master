@param org.kendar.ui.dto.ProtocolDto element

!{
    var sortedData = element.getPlugins().stream().
    sorted(java.util.Comparator.comparing(org.kendar.ui.dto.PluginDto::getId)).toList();
 }

@if(!(boolean) element.getParameters().get("avoidScript"))
    <script type="text/javascript">
        function reloadProtocol${element.getSafeInstanceId()}(callback) {
            console.log("reload${element.getInstanceId()}")
            var isOpen = isAccordionOpen('collapse${element.getSafeInstanceId()}');
            return htmx.ajax('GET', '/plugins/protocols?protocolId=${element.getInstanceId()}&avoidScript=true', '#${element.getSafeInstanceId()}panel').then(() => {
                if (isOpen) openAccordion('collapse${element.getSafeInstanceId()}')
            })
        }
    </script>
@endif

<div id="${element.getSafeInstanceId()}panel" class="panel panel-default">

    <div class="panel-heading" role="tab" id="heading${element.getSafeInstanceId()}">
        <h4 class="panel-title">
            <a class="collapsed" role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapse${element.getSafeInstanceId()}"
               aria-expanded="true"
               onclick="toggleAccordion('collapse${element.getSafeInstanceId()}')"
               aria-controls="collapse${element.getSafeInstanceId()}">
                Protocol <b>${element.getInstanceId()}</b> ( ${element.getProtocol()} )
            </a>
        </h4>
    </div>
    <div id="collapse${element.getSafeInstanceId()}" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="heading${element.getSafeInstanceId()}">
        <div class="panel-body">
            <table class="table table-bordered table-striped">
                <tr>
                    <th>Instance</th>
                    <th>Plugin</th>
                    <th>Status</th>
                    <th>Manage</th>
                </tr>
                @for(var plugin : sortedData)
                    <tr>
                        <td>${plugin.getInstanceId()}</td>
                        <td>${plugin.getId()}</td>
                        <td>
                            @if(plugin.isActive())
                                <button type="button" class="btn  btn-danger"
                                        onclick="getData('/api/protocols/${plugin.getInstanceId()}/plugins/${plugin.getId()}/stop','GET',()=>reloadProtocol${element.getSafeInstanceId()}().then(()=>reloadWildcard()).then(()=>reloadActive()))">
                                    Stop
                                </button>
                            @else
                                <button type="button" class="btn btn-success"
                                        onclick="getData('/api/protocols/${plugin.getInstanceId()}/plugins/${plugin.getId()}/start','GET',()=>reloadProtocol${element.getSafeInstanceId()}().then(()=>reloadWildcard()).then(()=>reloadActive()))">
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