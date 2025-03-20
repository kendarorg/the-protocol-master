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
                <div class="panel-heading" role="tab" id="headingStatus">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseStatus"
                           onclick="toggleAccordion('collapseStatus')"
                           aria-expanded="true" aria-controls="collapseStatus">
                            ${data.getInstanceId()}/${data.getId()}
                        </a>
                    </h4>
                </div>
                <div id="collapseStatus" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingStatus">
                    <div class="panel-body">
                        <form>
                            <div class="input-group">
                                <label for="pluginId" class="control-label">Plugin Id</label>
                                <div>
                                    <input id="pluginId" type="text" readonly="true" size="30" class="form-control"
                                           value="${data.getId()}">
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
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingSettings">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseSettings"
                           onclick="toggleAccordion('collapseSettings')"
                           aria-expanded="true" aria-controls="collapseSettings">
                            Settings
                        </a>
                    </h4>
                </div>
                <div id="collapseSettings" class="panel-collapse collapse" role="tabpanel"
                     aria-labelledby="headingSettings">
                    <div class="panel-body">
                        <form>
                            <div class="input-group">
                                <label for="settings" class="control-label">Settings</label>
                                <div>
                                    <textarea id="settings" class="form-control" rows="10"
                                              cols="200">${data.getSettings()}</textarea>
                                </div>
                            </div>
                            <div class="input-group">
                                <div>
                                    <br>
                                    <button type="button" class="btn btn-danger"
                                            onclick="sendData('/api/global/plugins/${data.getId()}/settings','PUT',
                                                    retrieveValue('settings'),'application/json',()=>location.reload())">
                                        Update Settings
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            @template.optional(new org.kendar.ui.OptionalTemplate(
                data,"global/"+data.getId()+"/index.jte",null))
        </div>
    </div>
</div>



@template.footer()

</body>