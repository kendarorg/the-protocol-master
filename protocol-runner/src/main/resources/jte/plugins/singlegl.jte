@param org.kendar.ui.dto.SinglePluginDto data
<head>

    <title>The Protocol Master - Global - ${data.getId()}</title>
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
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseAvailableFunctions"

                           onclick="toggleAccordion('collapseAvailableFunctions')"
                           aria-expanded="true" aria-controls="collapseAvailableFunctions">
                            Global/${data.getId()}
                        </a>
                    </h4>
                </div>
                <div id="collapseAvailableFunctions" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingAvailableFunctions">
                    <div class="panel-body">
                        <form>
                            <div class="input-group">
                                <label for="pluginId" class="control-label">Plugin Id</label>
                                <div>
                                    <input id="pluginId" type="text" readonly="true" size="30"  class="form-control" value="${data.getId()}">
                                </div>
                            </div>
                            <div class="input-group">
                                <label for="settings" class="control-label">Settings</label>
                                <div>
                                    <textarea id="settings" class="form-control" rows="10" cols="60" >${data.getSettings()}</textarea>
                                </div>
                            </div>
                            <div class="input-group">
                                <div>
                                    <br>
                                    @if(data.isActive())
                                        <button type="button" class="btn  btn-danger"
                                                onclick="getData('/api/global/plugins/${data.getId()}/stop','GET',()=>location.reload())">
                                            Stop
                                        </button>
                                    @else
                                        <button type="button" class="btn btn-success"
                                                onclick="getData('/api/global/plugins/${data.getId()}/start','GET',()=>location.reload())">
                                            Start
                                        </button>
                                    @endif
                                    &nbsp;
                                    <button type="button" class="btn btn-danger"
                                            onclick="sendData('/api/global/plugins/${data.getId()}/settings','PUT',
                                                    document.getElementById('settings').value,'application/json',()=>location.reload())">
                                        Update Settings
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            @template.optional(new org.kendar.ui.OptionalTemplate(data,"global/"+data.getId()+"/index.jte"))
        </div>
    </div>
</div>


@template.footer()

</body>