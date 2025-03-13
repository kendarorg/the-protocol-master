@param org.kendar.ui.dto.ProtocolStatusDto data

<head>

    <title>The Protocol Master - Plugins</title>
    @template.head()
</head>
<body>
<script type="application/javascript">

    function reloadAll(){
        var promise = null;
        if (typeof reloadActive === 'function'){
            promise = reloadActive();
        }
        if (typeof reloadWildcard === 'function'){
            if(promise!=null){
                promise.then(()=>reloadWildcard());
            }else {
                promise = reloadWildcard();
            }
        }
        @for(var element : data.getProtocols())
        if (typeof reloadProtocol${element.getSafeInstanceId()} === 'function'){
            if(promise!=null){
                promise.then(()=>reloadProtocol${element.getSafeInstanceId()}());
            }else {
                promise = reloadProtocol${element.getSafeInstanceId()}();
            }
        }
        @endfor
        return promise;
    }
</script>
@template.header("plugins")
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <!--
            ACTIVE PLUGINS
            -->
            <div hx-get="/plugins/active" hx-trigger="load"></div>


            <!--
            WILDCARD PLUGIN ACTIVATION PLUGINS
            -->
            <div hx-get="/plugins/wildcard" hx-trigger="load"></div>
            <!--
            PROTOCOL PLUGINS
            -->
            @for(var element : data.getProtocols())
                <div hx-get="/plugins/protocols?protocolId=${element.getInstanceId()}" hx-trigger="load"></div>
            @endfor
        </div>
    </div>
</div>
@template.footer()

</body>