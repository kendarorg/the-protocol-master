@import org.kendar.http.plugins.HttpErrorPluginSettings

@param HttpErrorPluginSettings settings

<table>
    <tr><td><label for="getShowError">Error Code</label></td><td><input type="text" id="getShowError" name="getShowError" value="${settings.getShowError()}"/></td></tr>
    <tr><td><label for="getErrorPercent">% Error Rate</label></td><td><input type="text" id="getErrorPercent" name="getErrorPercent" value="${settings.getErrorPercent()}"/></td></tr>
    <tr><td><label for="getErrorMessage">Error Message</label></td><td><input type="text" id="getErrorMessage" name="getErrorMessage"  value="${settings.getErrorMessage()}"/></td></tr>
</table>