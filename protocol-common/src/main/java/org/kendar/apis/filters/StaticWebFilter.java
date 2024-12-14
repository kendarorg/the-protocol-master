package org.kendar.apis.filters;


import com.fasterxml.jackson.databind.node.BinaryNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.kendar.annotations.HttpMethodFilter;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.utils.ConstantsHeader;
import org.kendar.apis.utils.ConstantsMime;
import org.kendar.apis.utils.MimeChecker;
import org.kendar.utils.FileResourcesUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StaticWebFilter implements FilteringClass {
    private final FileResourcesUtils fileResourcesUtils;
    private final ConcurrentHashMap<String, String> markdownCache = new ConcurrentHashMap<>();
    private HashMap<String, Object> resourceFiles = new HashMap<>();

    public StaticWebFilter(FileResourcesUtils fileResourcesUtils) {
        this.fileResourcesUtils = fileResourcesUtils;
        try {
            loadAllStuffs();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getPath();

    public String getDescription() {
        return null;
    }

    public String getAddress() {
        return null;
    }

    //TODO @PostConstruct
    public void loadAllStuffs() throws IOException {
        var realPath = getPath();
        if (isResource(getPath())) {
            realPath = realPath.substring(1);
            resourceFiles = fileResourcesUtils.loadResources(this, realPath);
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    @HttpMethodFilter(
            pathAddress = "*", method = "GET")
    public boolean handle(Request request, Response response) {
        var realPath = getPath();
        if (isResource(getPath())) {
            realPath = realPath.substring(1);
        }
        var requestedPath = request.getPath();
        if (requestedPath.endsWith("/")) {
            requestedPath = requestedPath.substring(0, requestedPath.length() - 1);
        }

        if (verifyPathAndRender(response, realPath, requestedPath, false)) return true;
        if (verifyPathAndRender(response, realPath, requestedPath + "/index.htm", true)) return true;
        if (verifyPathAndRender(response, realPath, requestedPath + "/index.html", true)) return true;
        if (verifyPathAndRender(response, realPath, request.getPath() + ".htm", true)) return true;
        if (verifyPathAndRender(response, realPath, request.getPath() + ".html", true)) return true;

        return false;
    }

    private boolean verifyPathAndRender(Response response, String realPath, String possibleMatch, boolean redirect) {
        Path fullPath;
        if (resourceFiles == null || resourceFiles.isEmpty()) {
            fullPath = Path.of(fileResourcesUtils.buildPath(realPath, possibleMatch));
        } else {
            fullPath = Path.of(realPath, possibleMatch);
        }
        if (isFileExisting(fullPath)) {
            if (redirect) {
                response.addHeader("location", possibleMatch);
                response.setStatusCode(302);
            } else {
                renderFile(fullPath, response);
            }
            return true;
        }
        return false;
    }

    private boolean isFileExisting(Path fullPath) {
        if (fullPath == null) return false;
        var resourcePath = fullPath.toString().replace('\\', '/');
        if (resourceFiles == null || resourceFiles.isEmpty()) {
            return Files.exists(fullPath) && !Files.isDirectory(fullPath);
        } else if (resourceFiles.containsKey(resourcePath)) {
            var data = resourceFiles.get(resourcePath);
            if (data == null) return false;
            return ((byte[]) data).length > 0;
        } else {
            return false;
        }
    }

    private boolean isResource(String path) {
        return path.startsWith("*");
    }

    private void renderFile(Path fullPath, Response response) {
        try {
            var stringPath = fullPath.toString();
            String mimeType = null;
            if (resourceFiles == null || resourceFiles.isEmpty()) {
                mimeType = Files.probeContentType(fullPath);
            }
            if (mimeType == null) {
                if (stringPath.endsWith(".js")) {
                    mimeType = "text/javascript";
                } else if (stringPath.endsWith(".css")) {
                    mimeType = "text/css";
                } else if (stringPath.endsWith(".htm") || stringPath.endsWith(".html")) {
                    mimeType = ConstantsMime.HTML;
                } else if (stringPath.endsWith(".md")) {
                    mimeType = ConstantsMime.HTML;
                } else {
                    mimeType = ConstantsMime.STREAM;
                }
            }
            var isBinary = (MimeChecker.isBinary(mimeType, null));
            if (resourceFiles == null || resourceFiles.isEmpty()) {
                renderRealFile(fullPath, response, stringPath, isBinary);
            } else {
                renderResourceFile(response, stringPath, isBinary);
            }
            response.addHeader(ConstantsHeader.CONTENT_TYPE, mimeType);
            response.setStatusCode(200);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderResourceFile(Response response, String stringPath, boolean isBinary) {
        var resourcePath = stringPath.replace('\\', '/');
        if (isBinary) {
            response.setResponseText(new BinaryNode((byte[]) resourceFiles.get(resourcePath)));
        } else {
            response.setResponseText(new TextNode(new String((byte[]) resourceFiles.get(resourcePath))));
        }
    }

    private void renderRealFile(Path fullPath, Response response, String stringPath, boolean isBinary) throws IOException {
        if (isBinary) {
            response.setResponseText(new BinaryNode(Files.readAllBytes(fullPath)));
        } else {
            response.setResponseText(new TextNode(Files.readString(fullPath)));
        }
    }
}
