package org.kendar.testcontainer.images;

import org.kendar.testcontainer.utils.BaseImage;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaImage extends BaseImage<JavaImage, GenericContainer> {

    private final Map<String,String> paths = new HashMap<>();
    private final List<String> dirs = new ArrayList<>();
    private String cmd;


    public JavaImage withDir(String dir) {
        dirs.add(dir);
        return this;
    }

    public JavaImage withCmd(String source,String destDir) {
        paths.put(Path.of(source).toAbsolutePath().toString(),destDir);
        this.cmd = destDir;
        return this;
    }

    public JavaImage withFile(String source,String destination) {
        paths.put(Path.of(source).toAbsolutePath().toString(),destination);
        return this;
    }

    @Override
    protected void preStart() {

        container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder -> {
                                    builder
                                            .from("amazoncorretto:11.0.23");
                                            for (var dir : dirs) {
                                                builder.run("mkdir -p " + dir);
                                            }
                                            builder.cmd("sh", "-c",cmd).build();
                                }
                        ));
        for(var path:paths.entrySet()){
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(
                            Path.of(path.getKey()).normalize(), 777),
                    path.getValue());
        }
        container.waitingFor(new WaitAllStrategy().withStartupTimeout(Duration.ofSeconds(5)));

    }
}
