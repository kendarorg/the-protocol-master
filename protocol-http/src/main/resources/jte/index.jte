@import org.kendar.http.api.ProtocolIndex

@param ProtocolIndex protocols


<head>
    <title>Error plugin settings</title>
</head>
<body>
<h2>${protocols.getProtocol()} Protocol instance ${protocols.getInstanceId()}</h2>

<ul>
    @for(var element : protocols.getProtocols())
        <li><a href="./${protocols.getInstanceId()}/plugins/${element}">Plugin ${element}</a></li>
    @endfor
</ul>
<script src="/lib/htmx.min.js"></script>
<script src="/lib/utils.js"></script>
</body>
