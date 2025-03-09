package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class FileTreeItemDto {
    public String getPath() {
        return path;
    }

    public String getSafeIndex() {
        return path.replaceAll("/", "__")+"__"+name;
    }


    private boolean open;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    private final String path;
    private List<FileTreeItemDto> children = new ArrayList<>();
    private String name;
    private boolean directory;

    public FileTreeItemDto(String path, boolean directory) {
        this.directory = directory;
        if(path.isEmpty()){
            this.path = "";
            this.name = "root";
        }else {
            var fakePath = new ArrayList<>(List.of(path.split("/")));
            this.name = fakePath.get(fakePath.size()-1);
            fakePath.remove(fakePath.size()-1);
            this.path = String.join("/", fakePath);
        }
    }

    public FileTreeItemDto(String root,String name, boolean directory) {
        this.name = name;
        this.directory = directory;
        this.path = root.length()>0?root+"/"+name:name;
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
