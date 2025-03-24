@param java.util.List<org.kendar.settings.ProtocolSettings> sortedProtocol

!{
    var safeId = new java.util.function.Function<org.kendar.settings.ProtocolSettings,String>(){
            @Override
            public String apply(org.kendar.settings.ProtocolSettings protocolSettings) {
                return protocolSettings.getProtocolInstanceId().replaceAll("-","");
            }
        };
}


<head>

    <title>The Protocol Master - Protocols</title>
    @template.head()
</head>
<body>
<script type="text/javascript">

</script>
@template.header("protocols")
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            @for(var protocol :   sortedProtocol)
                @template.optional(new org.kendar.ui.OptionalTemplate(
                                protocol,protocol.getProtocol()+"/protocol.jte",
                                "generic/protocol.jte"))

            @endfor
        </div>
    </div>
</div>
@template.footer()

</body>