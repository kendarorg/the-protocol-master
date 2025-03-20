<head>

    <title>The Protocol Master - Storage</title>
    @template.head()
</head>
<body>
@template.header("recording")

<script type="text/javascript">

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

                    </div>
                </div>
            </div>
        </div>

    </div>
</div>

@template.footer()

</body>