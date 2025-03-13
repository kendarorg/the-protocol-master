@param org.kendar.ui.dto.ProtocolStatusDto data

<head>

    <title>The Protocol Master - Plugins</title>
    @template.head()
</head>
<body>
<script type="application/javascript">

    function reloadWildcard(callback) {
        console.log("reloadWildcard")
        var isOpen = isAccordionOpen('collapseWildcard');
        return htmx.ajax('GET', '/plugins/wildcard?avoidScript=true', '#wildcardPanel').then(() => {
            if (isOpen) openAccordion('collapseWildcard');
        })
    }

    function reloadActive(callback) {
        console.log("reloadActive")
        var isOpen = isAccordionOpen('collapseActive');
        return htmx.ajax('GET', '/plugins/active?avoidScript=true', '#activePanel')
            .then(() => {
                if (isOpen) openAccordion('collapseActive');
            })
    }

    @for(var element : data.getProtocols())
    function reloadProtocol${element.getSafeInstanceId()}(callback){
        console.log("reload${element.getInstanceId()}")
        var isOpen = isAccordionOpen('collapse${element.getSafeInstanceId()}');
        return htmx.ajax('GET', '/plugins/protocols?protocolId=${element.getInstanceId()}&avoidScript=true', '#${element.getSafeInstanceId()}panel').
        then(()=>{
            if(isOpen)openAccordion('collapse${element.getSafeInstanceId()}')
        })
    }
    @endfor

    function reloadAllPlugins(){
        var promise = reloadActive();
        promise.then(()=>reloadWildcard());
        @for(var element : data.getProtocols())
        promise.then(()=>reloadProtocol${element.getSafeInstanceId()}());
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