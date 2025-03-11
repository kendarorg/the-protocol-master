@param String currentPage


<nav class="navbar navbar-default">
    <div class="container-fluid">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
            <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1" aria-expanded="false">
                <span class="sr-only">Toggle navigation</span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
                <span class="icon-bar"></span>
            </button>
            <a class="navbar-brand" href="#">TPM</a>
        </div>

        <!-- Collect the nav links, forms, and other content for toggling -->
        <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">

                <!--
                MAIN
                -->
                @if(currentPage.startsWith("index"))
                    <li class="active" ><a  href="/">Main</a></li>
                @else
                    <li><a href="/">Main</a></li>
                @endif

                <!--
                STORAGE
                -->
                @if(currentPage.startsWith("storage"))
                    <li class="active" ><a  href="/storage">Storage</a>
                    </li>
                @else
                    <li><a  href="/storage">Storage</a>
                    </li>
                @endif

                <!--
                PLUGINS
                -->
                @if(currentPage.startsWith("plugins"))
                    <li class="active" ><a  href="/plugins">Plugins</a></li>
                @else
                    <li><a href="/plugins">Plugins</a></li>
                @endif
                <!--
                GLOBAL PLUGINS
                -->
                @if(currentPage.startsWith("globalpl"))
                    <li class="active" ><a  href="/globalpl">Global-Plugins</a></li>
                @else
                    <li><a href="/globalpl">Global-Plugins</a></li>
                @endif
                    <li><a href="/swagger-ui/index.html#/">OpenAPI</a></li>
<%--                <li><a href="/storage">Storage</a></li>--%>
<%--                <li><a @if(currentPage.startsWith("plugins"))class="active" @endif href="/plugins">Plugins</a></li>--%>
<%--                <li><a @if(currentPage.startsWith("globalpl"))class="active" @endif href="/globalpl">Plugins-Global</a></li>--%>

<%--                <li class="active"><a href="#">Link <span class="sr-only">(current)</span></a></li>--%>
<%--                <li><a href="#">Link</a></li>--%>
<%--                <li class="dropdown">--%>
<%--                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Dropdown <span class="caret"></span></a>--%>
<%--                    <ul class="dropdown-menu">--%>
<%--                        <li><a href="#">Action</a></li>--%>
<%--                        <li><a href="#">Another action</a></li>--%>
<%--                        <li><a href="#">Something else here</a></li>--%>
<%--                        <li role="separator" class="divider"></li>--%>
<%--                        <li><a href="#">Separated link</a></li>--%>
<%--                        <li role="separator" class="divider"></li>--%>
<%--                        <li><a href="#">One more separated link</a></li>--%>
<%--                    </ul>--%>
<%--                </li>--%>
            </ul>
            <form class="navbar-form navbar-left">
<%--                <div class="form-group">--%>
<%--                    <input type="text" class="form-control" placeholder="Search">--%>
<%--                </div>--%>
<%--                <button type="submit" class="btn btn-default">Submit</button>--%>
            </form>
            <ul class="nav navbar-nav navbar-right">
<%--                <li><a href="#">Link</a></li>--%>
<%--                <li class="dropdown">--%>
<%--                    <a href="#" class="dropdown-toggle" data-toggle="dropdown" role="button" aria-haspopup="true" aria-expanded="false">Dropdown <span class="caret"></span></a>--%>
<%--                    <ul class="dropdown-menu">--%>
<%--                        <li><a href="#">Action</a></li>--%>
<%--                        <li><a href="#">Another action</a></li>--%>
<%--                        <li><a href="#">Something else here</a></li>--%>
<%--                        <li role="separator" class="divider"></li>--%>
<%--                        <li><a href="#">Separated link</a></li>--%>
<%--                    </ul>--%>
<%--                </li>--%>
            </ul>
        </div><!-- /.navbar-collapse -->
    </div><!-- /.container-fluid -->
</nav>

<div id="modal-1" class="jw-modal">
    <div class="jw-modal-body">
        <div id="modal-content">

        </div>
        <button onclick="closeModal()">Close</button>
    </div>
</div>