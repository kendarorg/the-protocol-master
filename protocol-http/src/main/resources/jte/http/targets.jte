@param org.kendar.http.plugins.apis.dtos.TargetsContainer targetsContainer

<br>
<div class="input-group">
                <span class="input-group-btn">
                    <button class="btn btn-default" type="button"
                            onclick="addHost()">
                        Create</button>
                </span>
    <input id="createNewTarget" type="text" size="30"
           class="form-control">
</div>
<br>
<div class="input-group">
                <span class="input-group-btn">
                    <button class="btn btn-default" type="button"
                            onclick="verifyHost()">
                        Verify</button>
                </span>
    <input id="verifyMatch" type="text" size="30"
           class="form-control">
</div>
<br>
<table class="table table-bordered table-striped">
    <tr>
        <th>Target</th>
        <th></th>
    </tr>
    @for(var i=0;i<targetsContainer.getTarget().size();i++)
        <tr>
            <td>
                <input type="hidden" value=" ${targetsContainer.getTarget().get(i)}"
                       id="target_${1}">
                ${targetsContainer.getTarget().get(i)}
            </td>
            <td>
                <button class="btn btn-danger" type="button"
                        onclick="removeHost('${targetsContainer.getTarget().get(i)}')">Remove
                </button>
            </td>
        </tr>
    @endfor
</table>


<script type="text/javascript">
    var pluginTargets = getAllTargets();
    function addHost(){
        var toAdd = retrieveValue('createNewTarget');
        pluginTargets.push(toAdd);
        ${targetsContainer.getApi()}();
    }

    function verifyHost(){
        var toSend = {
            target:retrieveValue('createNewTarget'),
            matchAgainst:retrieveValue('verifyMatch')
        };
        sendData('/api/protocols/http/targets/verify', 'POST', JSON.stringify(toSend), 'application/json', (status,response) => {
            if(status==200){
                alert("Pattern mathcing!");
            }else if(status==404){
                alert("Pattern not mathcing!");
            }else if(status==500){
                alert("Error! "+response);
            }

        })
    }
    function removeHost(host){
        if (confirm("You want to remove " + host + "?") == true) {
            for (let i = (pluginTargets.length - 1); i >= 0; i--) {
                if (pluginTargets[i] == host) {
                    pluginTargets = pluginTargets.splice(i, 1);
                    break;
                }
            }
            ${targetsContainer.getApi()}();
        }
    }
    function getAllTargets(){
        var result =[];
        for(var i=0;i<100;i++){
            try{
                var value = retrieveValue('target_'+i);
                result.push(value);
            }catch (error){
                break;
            }
        }
        return result;
    }
</script>