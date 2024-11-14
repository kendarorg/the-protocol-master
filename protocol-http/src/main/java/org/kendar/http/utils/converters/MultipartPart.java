package org.kendar.http.utils.converters;

import org.apache.commons.fileupload.FileItem;
import org.kendar.http.utils.MimeChecker;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MultipartPart {
    private boolean file;
    private String contentType;
    private String fileName;
    private String fieldName;
    private String stringData;
    private byte[] byteData;
    private Map<String, String> headers = new HashMap<>();

    protected MultipartPart() {

    }

    public MultipartPart(FileItem fileItem) {
        this.contentType = fileItem.getContentType();
        this.file = !fileItem.isFormField();
        setFieldName(fileItem.getFieldName());
        if (fileItem.isFormField()) {
            setStringData(fileItem.getString());
        } else {
            setByteData(fileItem.get());
            setFileName(fileItem.getName());
        }
    }

    public MultipartPart(RequestUtils.SimpleBlock simpleBlock) {
        contentType = "text/plain";
        ContentDisposition fileItem = null;
        for (var h : simpleBlock.headers.entrySet()) {
            if (h.getKey().equalsIgnoreCase("Content-Type")) {
                contentType = h.getValue().trim();
            } else if (h.getKey().equalsIgnoreCase("Content-Disposition")) {
                fileItem = ContentDisposition.parse(h.getValue().trim());
            } else {
                headers.put(h.getKey(), h.getValue());
            }
        }

        if (this.contentType.contains(";")) {
            this.contentType = this.contentType.split(";", 2)[0].trim();
        }
        if (fileItem != null) {
            this.file = !fileItem.isFormData();
            setFieldName(fileItem.getName());
            if (fileItem.isFormData()) {
                if (MimeChecker.isBinary(this.contentType, null)) {
                    setByteData(simpleBlock.data);
                    setFileName(fileItem.getFilename());
                } else {
                    setStringData(new String(simpleBlock.data));
                    setFileName(fileItem.getFilename());
                }
            } else {
                setByteData(simpleBlock.data);
                setFileName(fileItem.getFilename());
            }
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String s) {
        if (getHeaders().containsKey(s)) {
            return getHeaders().get(s);
        }
        for (var kvp : getHeaders().entrySet()) {
            if (kvp.getKey().equalsIgnoreCase(s)) {
                return kvp.getValue();
            }
        }
        return null;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getStringData() {
        return stringData;
    }

    public void setStringData(String stringData) {
        this.stringData = stringData;
    }

    public byte[] getByteData() {
        return byteData;
    }

    public void setByteData(byte[] byteData) {
        this.byteData = byteData;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isFile() {
        return file;
    }

    public void setFile(boolean b) {
        file = b;
    }

    public MultipartPart copy() {
        var r = new MultipartPart();
        r.byteData = this.byteData != null ? this.byteData.clone() : this.byteData;
        r.contentType = this.contentType;
        r.fieldName = this.fieldName;
        r.file = this.file;
        r.fileName = this.fileName;
        r.stringData = this.stringData != null ? this.stringData : this.stringData;
        r.headers = this.headers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return r;
    }
}
