package org.kendar.tests.testcontainer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReformatIndexes {
    private static String getFileContent(Path file) throws IOException {
        return Files.readString(file);
    }

    //@Test
    void reformat() throws IOException {
        var path = Path.of("").toAbsolutePath().getParent();

        System.out.println(path);
        try (Stream<Path> walk = Files.walk(path)) {
            var result = walk
                    .filter(p -> !Files.isDirectory(p)
                            && !(p.toFile().getAbsolutePath().contains(".git"))
                            && !(p.toFile().getAbsolutePath().contains(".idea")))   // not a directory
                    .filter(p -> {
                        return p.toFile().getName().endsWith(".json") &&
                                p.toFile().getName().startsWith("0000");
                    })
                    .collect(Collectors.toList());

            System.out.println(result);
            ObjectMapper mapper = new ObjectMapper().
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            for (var file : result) {
                try {
                    var changed = false;
                    var content = getFileContent(file);
                    var tree = mapper.readTree(content);
                    if (tree.get("input") != null
                            && tree.get("input").get("data") != null
                            && tree.get("input").get("type") != null
                            && !tree.get("input").get("type").textValue().isEmpty()) {
                        var inputData = tree.get("input").get("data");
                        var type = tree.get("input").get("type").textValue();
                        ((ObjectNode) tree).put("input", inputData);
                        ((ObjectNode) tree).put("inputType", type);
                        changed = true;
                    }
                    if (tree.get("output") != null
                            && tree.get("output").get("data") != null
                            && tree.get("output").get("type") != null
                            && !tree.get("output").get("type").textValue().isEmpty()) {
                        var inputData = tree.get("output").get("data");
                        var type = tree.get("output").get("type").textValue();
                        ((ObjectNode) tree).put("output", inputData);
                        ((ObjectNode) tree).put("outputType", type);
                        changed = true;
                    }

                    if (changed) {

                        var newTree = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
                        System.out.println(newTree);
                        Files.writeString(file, newTree);
                    }
                } catch (Exception e) {

                }
            }
        }
    }
}
