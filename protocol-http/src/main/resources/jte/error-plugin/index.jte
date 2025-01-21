@import org.kendar.http.plugins.HttpErrorPluginSettings

@param HttpErrorPluginSettings settings

<!--
https://htmx.org/api/
https://coderonfleek.medium.com/sending-data-in-htmx-requests-3-ways-4287ead56dbd
https://htmx.org/attributes/hx-vals/
https://htmx.org/api/
-->
<head>
    <title>Error plugin settings</title>

</head>
<body>
<h2>Error plugin settings</h2>
<table id="formData">
    <tr><td><label for="showError">Error Code</label></td><td><input type="text" id="showError" name="showError" value="${settings.getShowError()}"/></td></tr>
    <tr><td><label for="errorPercent">% Error Rate</label></td><td><input type="text" id="errorPercent" name="errorPercent" value="${settings.getErrorPercent()}"/></td></tr>
    <tr><td><label for="errorMessage">Error Message</label></td><td><input type="text" id="errorMessage" name="errorMessage"  value="${settings.getErrorMessage()}"/></td></tr>
</table>
<button value="Submit"
        hx-target="#formData"
        hx-post="./error-plugin"
        hx-vals="js:{...formData()}"
        hx-ext="submitjson"/>
<script src="/lib/htmx.min.js"></script>
<script src="/lib/utils.js"></script>
<script type="application/javascript">
    formData= function(){
        debugger;
        return {
            showError:document.getElementById("showError").value,
            errorPercent:document.getElementById("errorPercent").value,
            errorMessage:document.getElementById("errorMessage").value,
        }
    }
</script>
</body>