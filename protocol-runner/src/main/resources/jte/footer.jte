<script type="text/javascript">
    function loadPanels() {

        var panelStatus = getAccordionStatus();
        var propertyNames = Object.getOwnPropertyNames(panelStatus);
        for (var i = 0; i < propertyNames.length; i++) {
            try {
                var propertyData = panelStatus[propertyNames[i]];
                var propertyName = propertyData.elementId;
                var propertyStatus = propertyData.status;
                var path = propertyData.path;
                if (path !== window.location.pathname) {
                    continue;
                }
                var panel = document.getElementById(propertyName);
                if (panel == null) {
                    continue;
                }

                if (propertyStatus === "open") {
                    openAccordion(propertyName);
                } else {
                    closeAccordion(propertyName);
                }
            } catch (error) {

            }
        }
    }

    setTimeout(function () {
        loadPanels();
    }, 500);
</script>
