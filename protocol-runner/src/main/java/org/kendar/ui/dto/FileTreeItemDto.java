package org.kendar.ui.dto;

import java.util.ArrayList;
import java.util.List;

public class FileTreeItemDto {
    private boolean open;
    private String path;

//    public String getRealPath(){
//        return path+"/"+name;
//    }
    private List<FileTreeItemDto> children = new ArrayList<>();
    //private String name;
    private boolean directory;

    public FileTreeItemDto(String path, boolean directory) {
        this.directory = directory;
        this.path = path;
//        if(path.isEmpty()){
//            this.path = "";
//            this.name = "root";
//        }else {
//            var fakePath = new ArrayList<>(List.of(path.split("/")));
//            this.name = fakePath.get(fakePath.size()-1);
//            fakePath.remove(fakePath.size()-1);
//            this.path = String.join("/", fakePath);*
//        }
    }

    public FileTreeItemDto(String root, String name, boolean directory) {
        //this.name = name;
        this.directory = directory;
        this.path = root.length() > 0 ? root + "/" + name : name;
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
        var ph = path.substring(path.lastIndexOf("/") + 1);
        return ph;
    }


    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }
}
