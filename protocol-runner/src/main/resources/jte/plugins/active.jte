@param org.kendar.ui.dto.ProtocolStatusDto data

@if(!(boolean) data.getParameters().get("avoidScript"))
    <script type="text/javascript">
        function reloadActive(callback) {
            console.log("reloadActive")
            var isOpen = isAccordionOpen('collapseActive');
            return htmx.ajax('GET', '/plugins/active?avoidScript=true', '#activePanel')
                .then(() => {
                    if (isOpen) openAccordion('collapseActive');
                })
        }
    </script>
@endif
<div id="activePanel" class="panel panel-default">

    <div class="panel-heading" role="tab" id="headingActive">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseActive"
               onclick="toggleAccordion('collapseActive')"
               aria-expanded="true" aria-controls="collapseActive">
                Active plugins
            </a>
        </h4>
    </div>
    <div id="collapseActive" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingActive">
        <div class="panel-body">
            <table class="table">
                <tr>
                    <th>Instance</th>
                    <th>Plugin</th>
                    <th>Stop</th>
                    <th>Manage</th>
                </tr>
                @for(var element : data.getActivePlugins())
                    <tr>
                        <td>${element.getInstanceId()}</td>
                        <td>${element.getId()}</td>
                        <td>
                            <button type="button" class="btn  btn-danger"
                                    onclick="getData('/api/protocols/${element.getInstanceId()}/plugins/${element.getId()}/stop','GET',reloadAllPlugins)">
                                Stop
                            </button>
                        </td>
                        <td>
                            <a class="btn" href="/plugins/${element.getInstanceId()}/${element.getId()}">Manage</a>
                        </td>
                    </tr>
                @endfor
            </table>
        </div>
    </div>
</div>