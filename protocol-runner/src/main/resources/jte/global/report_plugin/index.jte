@param org.kendar.ui.dto.SinglePluginDto data
<script>
    function searchReport(){
        var tpmql=document.getElementById('tpmqlQuery').value;
        var start=document.getElementById('tpmqlQueryStart').value;
        var limit=document.getElementById('tpmqlQueryLimit').value;
        htmx.ajax('GET',
            '/global/plugins/report-plugin/report/search?' +
            'tpmql='+encodeURI(tpmql)+"&start="+start+"&limit="+limit,
            {target:"#reportSearchResult", swap:'outerHTML'});
    }
</script>
<div class="panel panel-default">
    <div class="panel-heading" role="tab" id="headingReportData">
        <h4 class="panel-title">
            <a role="button" data-toggle="collapse" data-parent="#accordion"
               href="#collapseReportData"
               onclick="toggleAccordion('collapseReportData')"
               aria-expanded="true" aria-controls="collapseReportData">
                Report data browser
            </a>
        </h4>
    </div>
    <div id="collapseReportData" class="panel-collapse collapse" role="tabpanel"
         aria-labelledby="headingReportData">
        <div class="panel-body">
            <form>
                <div class="input-group">
                    <label for="tpmqlQueryStart" class="control-label">Start</label>
                    <div>
                        <input type="number" id="tpmqlQueryStart" class="form-control" value="0" />
                    </div>
                </div>
                <div class="input-group">
                    <label for="tpmqlQueryLimit" class="control-label">Limit</label>
                    <div>
                        <input type="number" id="tpmqlQueryLimit" class="form-control" value="10" />
                    </div>
                </div>
                <div class="input-group">
                    <label for="tpmqlQuery" class="control-label">Query</label>
                    <div>
                        <textarea id="tpmqlQuery" class="form-control" rows="6" cols="70" >
SELECT(
    WHAT(date=MSTODATE(timestamp),instanceId,protocol,
        query=SUBSTR(query,50),duration,tags=WRAP(tags,50,' ')),
    WHERE(tags.method=='POST'),
    ORDERBY(DESC(date))
)</textarea>
                    </div>
                </div>
                <br>
                <div class="input-group">
                        <button type="button" class="btn btn-success"
                                onclick="searchReport()">
                            Search
                        </button>
                </div>
                <br>
                <div id="reportSearchResult"></div>
            </form>
        </div>
    </div>
</div>