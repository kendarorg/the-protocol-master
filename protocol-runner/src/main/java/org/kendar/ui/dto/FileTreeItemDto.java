package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class FileTreeItemDto {
    private List<FileTreeItemDto> children = new ArrayList<>();
    private String name;
    private boolean directory;

    public FileTreeItemDto(String name, boolean directory) {
        this.name = name;
        this.directory = directory;
    }

    public List<FileTreeItemDto> getChildren() {
        return children;
    }

    public void setChildren(List<FileTreeItemDto> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
}
