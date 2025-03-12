@param org.kendar.ui.dto.SinglePluginDto data

!{var pluginSettings = (org.kendar.http.plugins.settings.SSLDummyPluginSettings) data.getSettingsObject();}

    <div class="panel panel-default">
        <div class="panel-heading" role="tab" id="headingSpecificPlugin">
            <h4 class="panel-title">
                <a role="button" data-toggle="collapse" data-parent="#accordion"
                   href="#collapseSpecificPlugin"
                   onclick="toggleAccordion('collapseSpecificPlugin')"
                   aria-expanded="true" aria-controls="collapseSpecificPlugin">
                    SSL Certificates
                </a>
            </h4>
        </div>
        <div id="collapseSpecificPlugin" class="panel-collapse collapse" role="tabpanel"
             aria-labelledby="headingSpecificPlugin">
            <div class="panel-body">
                <form>
                    <div class="input-group">
                        <label for="instanceId" class="control-label">Instance Id</label>
                        <div>
                            <input id="instanceId" type="text" readonly="true" size="30" class="form-control"
                                   value="${data.getInstanceId()}">
                        </div>
                    </div>
                </form>

                adaf
<%--                <form>--%>
<%--                    <div class="input-group">--%>
<%--                        <label for="instanceId" class="control-label">Instance Id</label>--%>
<%--                        <div>--%>
<%--                            <input id="instanceId" type="text" readonly="true" size="30" class="form-control"--%>
<%--                                   value="${data.getInstanceId()}">--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                    <div class="input-group">--%>
<%--                        <label for="pluginId" class="control-label">Plugin Id</label>--%>
<%--                        <div>--%>
<%--                            <input id="pluginId" type="text" readonly="true" size="30" class="form-control"--%>
<%--                                   value="${data.getId()}">--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                    <div class="input-group">--%>
<%--                        <label for="settings" class="control-label">Settings</label>--%>
<%--                        <div>--%>
<%--                                    <textarea id="settings" class="form-control" rows="10"--%>
<%--                                              cols="60">${data.getSettings()}</textarea>--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                    <div class="input-group">--%>
<%--                        <div>--%>
<%--                            <br>--%>
<%--                            @if(data.isActive())--%>
<%--                                <button type="button" class="btn  btn-danger"--%>
<%--                                        onclick="getData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/stop','GET',()=>location.reload())">--%>
<%--                                    Stop--%>
<%--                                </button>--%>
<%--                            @else--%>
<%--                                <button type="button" class="btn btn-success"--%>
<%--                                        onclick="getData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/start','GET',()=>location.reload())">--%>
<%--                                    Start--%>
<%--                                </button>--%>
<%--                            @endif--%>
<%--                            &nbsp;--%>
<%--                            <button type="button" class="btn btn-danger"--%>
<%--                                    onclick="sendData('/api/protocols/${data.getInstanceId()}/plugins/${data.getId()}/settings','PUT',--%>
<%--                                            document.getElementById('settings').value,'application/json',()=>location.reload())">--%>
<%--                                Update Settings--%>
<%--                            </button>--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                </form>--%>
            </div>
        </div>
    </div>