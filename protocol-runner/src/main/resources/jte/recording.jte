<head>

    <title>The Protocol Master - Storage</title>
    @template.head()
</head>
<body>
@template.header("recording")

<script type="text/javascript">
    function searchRecording(){
        var tpmql = retrieveValue('tpmqlQuery');
        var start = retrieveValue('tpmqlQueryStart');
        var limit = retrieveValue('tpmqlQueryLimit');
        htmx.ajax('GET',
            '/recording/search?' +
            'tpmql=' + encodeURI(tpmql) + "&start=" + start + "&limit=" + limit,
            {target: "#recordingSearchResult", swap: 'outerHTML'});
    }
    function openById(id){
        openAccordion('collapseStorageSingleFile')
        htmx.ajax('GET',
            '/recording/search/' +id,
            {target: "#recordingSingleFile", swap: 'outerHTML'});
    }
</script>
<div class="panel panel-default">
    <div class="panel-body">
        <div class="panel-group" id="accordion" role="tablist" aria-multiselectable="true">
            <!--
            HANDLE RECORDING
            -->

            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingStorage">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseStorage"
                           onclick="toggleAccordion('collapseStorage')"
                           aria-expanded="true" aria-controls="collapseStorage">
                            Recording Manager
                        </a>
                    </h4>
                </div>
                <div id="collapseStorage" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingStorage">
                    <div class="panel-body">
                        <div class="input-group">
                            <label for="tpmqlQueryStart" class="control-label">Start</label>
                            <div>
                                <input type="number" id="tpmqlQueryStart" class="form-control" value="0"/>
                            </div>
                        </div>
                        <div class="input-group">
                            <label for="tpmqlQueryLimit" class="control-label">Limit</label>
                            <div>
                                <input type="number" id="tpmqlQueryLimit" class="form-control" value="10"/>
                            </div>
                        </div>
                        <div class="input-group">
                            <label for="tpmqlQuery" class="control-label">Query. `index` field must be present to open the record</label>
                            <div>
                        <textarea id="tpmqlQuery" class="form-control" rows="10" cols="200">
SELECT(
    WHAT(
                index=index.index,
		date=MSTODATE(index.timestamp)
		index=index.index,
		instance=index.protocolInstanceId,
		type=index.type
        query=SUBSTR(index.query,50),
		durationMs=data.durationMs,
		tags=SUBSTR(WRAP(index.tags,50,' '),100)),
    ORDERBY(ASC(index))
)</textarea>
                            </div>
                        </div>
                        <br>
                        <div class="input-group">
                            <button type="button" class="btn btn-success"
                                    onclick="searchRecording()">
                                Search
                            </button>
                        </div>
                        <br>
                        <div id="recordingSearchResult"></div>
                    </div>
                </div>
            </div>

            <div class="panel panel-default">
                <div class="panel-heading" role="tab" id="headingStorageSingleFile">
                    <h4 class="panel-title">
                        <a role="button" data-toggle="collapse" data-parent="#accordion"
                           href="#collapseStorageSingleFile"
                           onclick="toggleAccordion('collapseStorageSingleFile')"
                           aria-expanded="true" aria-controls="collapseStoragSingleFilee">
                            Single file
                        </a>
                    </h4>
                </div>
                <div id="collapseStorageSingleFile" class="panel-collapse collapse in" role="tabpanel"
                     aria-labelledby="headingStorageSingleFile">
                    <div id="recordingSingleFile"></div>
                </div>
            </div>
        </div>

    </div>
</div>


@template.footer()

</body>