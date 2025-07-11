package org.kendar.sample.m3u;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class M3uFile {
    private String tvgShift;
    private List<M3uEntity> entities = new ArrayList<>();

    public void parse(String file) {
        var lines = file.split("\n");
        M3uEntity prev = null;
        for (String line : lines) {
            line = line.trim();
            if(line.isEmpty()) continue;
            if(line.startsWith("#")) {
                var entity = new M3uEntity();
                entity.parse(line);
                entities.add(entity);
                if(prev == null) {
                    if(entity.getAttributes().containsKey("tvgShift")){
                        this.tvgShift= entity.getAttributes().get("tvgShift");
                    }
                }else{
                    if(entity.getType()==EntityType.INF && this.tvgShift!=null){
                        entity.getAttributes().put("tvgShift", this.tvgShift);
                    }
                }
                prev = entity;
            }else{
                if(prev!=null && (prev.getType()==EntityType.INF||prev.getType()==EntityType.STREAM_INF)){
                    prev.getAttributes().put("channelUrl", line);
                }
            }
        }
    }

    public List<M3uEntity> findAll(EntityType entityType) {
        return entities.stream().filter(entity -> entity.getType()==entityType).collect(Collectors.toList());
    }
}
