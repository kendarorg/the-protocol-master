package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class FileTreeItemDto {
    private boolean open;
    private final String path;

    private List<FileTreeItemDto> children = new ArrayList<>();
    private boolean directory;

    public FileTreeItemDto(String path, boolean directory) {
        this.directory = directory;
        this.path = path;
    }

    public FileTreeItemDto(String root, String name, boolean directory) {
        this.directory = directory;
        this.path = !root.isEmpty() ? root + "/" + name : name;
    }

    public String getPath() {
        return path;
    }

    public String getSafePath() {
        return path.replaceAll("/", "__").replaceAll("-", "_");
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public List<FileTreeItemDto> getChildren() {
        return children;
    }

    public void setChildren(List<FileTreeItemDto> children) {
        this.children = children;
    }

    public String getName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }


    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
}
