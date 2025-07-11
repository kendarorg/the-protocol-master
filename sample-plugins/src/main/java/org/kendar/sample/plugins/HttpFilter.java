package org.kendar.sample.plugins;

import org.apache.commons.codec.binary.Hex;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.buffers.BBuffer;
import org.kendar.di.annotations.TpmService;
import org.kendar.plugins.base.AlwaysActivePlugin;
import org.kendar.plugins.base.ProtocolPhase;
import org.kendar.plugins.base.ProtocolPluginDescriptorBase;
import org.kendar.proxy.PluginContext;
import org.kendar.sample.m3u.EntityType;
import org.kendar.sample.m3u.M3uEntity;
import org.kendar.sample.m3u.M3uFile;
import org.kendar.utils.JsonMapper;
import org.pf4j.Extension;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A plugin that filters HTTP requests and responses based on specific conditions.
 * It processes M3U files and generates scripts for downloading and merging video streams.
 */
@Extension
@TpmService(tags = "http")
public class HttpFilter extends ProtocolPluginDescriptorBase<HttpFilterSettings> implements AlwaysActivePlugin {
    private static String episode = "test";
    private static final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private String folder;

    /**
     * Constructor for HttpFilter.
     *
     * @param mapper The JsonMapper instance used for JSON operations.
     */
    public HttpFilter(JsonMapper mapper) {
        super(mapper);
    }

    /**
     * Fetches the HTML content of a given URL.
     *
     * @param urlToRead The URL to fetch the HTML content from.
     * @return The HTML content as a String.
     */
    public static String getHTML(String urlToRead) {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    result.append(line + "\n");
                }
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fetches binary data from a given URL.
     *
     * @param urlToRead The URL to fetch the binary data from.
     * @return The binary data as a byte array.
     */
    public static byte[] getBinary(String urlToRead) {
        var result = new BBuffer();
        try (BufferedInputStream in = new BufferedInputStream(new URL(urlToRead).openStream())) {

            byte[] dataBuffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 4096)) != -1) {
                result.writePartial(dataBuffer, bytesRead);
            }
        } catch (IOException e) {
            // handle exception
        }
        return result.toArray();
    }

    /**
     * Pads a string with leading zeros to match the specified length.
     *
     * @param inputString The input string to pad.
     * @param length      The desired length of the output string.
     * @return The padded string.
     */
    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    private AtomicInteger counter = new AtomicInteger(0);

    /**
     * Handles HTTP requests and responses during specific protocol phases.
     *
     * @param pluginContext The plugin context.
     * @param phase         The current protocol phase.
     * @param in            The incoming request.
     * @param out           The outgoing response.
     * @return True if the request was handled, false otherwise.
     */
    public boolean handle(PluginContext pluginContext, ProtocolPhase phase, Request in, Response out) {

        /*for (var item : cache.values()) {
            if (in.getHost().toLowerCase().contains(item.toLowerCase())) {
                out.setStatusCode(404);
                return true;
            }
        }
        if (in.getHost().contains("tagru") || in.getHost().contains("toro")
                || in.getHost().contains("casino") || in.getHost().contains("porn")
                || in.getHost().contains("sex") || in.getHost().contains("bet")
                || in.getHost().contains(".bet")|| in.getHost().contains("roudoduor")) {
            out.setStatusCode(404);
            return true;
        }*/
            if (phase == ProtocolPhase.POST_CALL) {
                try {
                    System.out.println("POST");
                    if (out.getHeader("content-type") != null &&
                            out.getHeader("content-type").contains("application/json")) {
                        var json = out.getResponseText();

                        if (json.get("component") != null &&
                                (json.get("component").textValue().equals("Titles/Title") ||
                                json.get("component").textValue().equals("Titles/Watch"))) {
                            if (json.get("props") != null) {
                                if (json.get("props").get("episode") != null) {
                                    if (json.get("props").get("episode").get("season").get("number") != null) {
                                        String convertedString =
                                                Normalizer
                                                        .normalize(json.get("props").get("episode").get("name").asText().replaceAll("/","-")
                                                                .replaceAll(":","-")
                                                                .replaceAll("!","-")
                                                                .replaceAll("\\?","-"), Normalizer.Form.NFD)
                                                        .replaceAll("[^\\p{ASCII}]", "");
                                        episode =
                                                "S" + padLeftZeros(json.get("props").get("episode").get("season").get("number").asText(), 2) + ""
                                                        + "E" + padLeftZeros(json.get("props").get("episode").get("number").asText(), 2) + " " +
                                                        convertedString;
                                        folder = "S" + padLeftZeros(json.get("props").get("episode").get("season").get("number").asText(), 2) + ""
                                                + "E" + padLeftZeros(json.get("props").get("episode").get("number").asText(), 2);

                                    }

                                }
                            }
                        }
                        System.out.println("M3U");
                    }
                }catch (Exception e){

                }

                if (in.buildUrl().contains("kendar")) {
                    if (in.getQuery("episode") != null) {
                        //episode = in.getQuery("episode");
                    }
                }
                if (episode == null) {
                    //episode = getNow() + "";
                }
                var ct = out.getFirstHeader("content-type");
                if (ct == null) return false;
                if (ct.contains("application/vnd.apple.mpegurl")) {
                    if(episode.startsWith("test")){
                        episode="test_"+counter.incrementAndGet();
                    }
                    var result = new String(Base64.getDecoder().decode(out.getResponseText().asText()));
                    var file = new M3uFile();
                    file.parse(result);
                    var allStreamInf = file.findAll(EntityType.STREAM_INF)
                            .stream().min(Comparator.comparing(M3uEntity::getBandwith));
                    if (allStreamInf.isPresent()) {
                        var maxStreamInf = allStreamInf.get();
                        var streamUm3u = getHTML(maxStreamInf.getAttributes().get("channelUrl"));
                        var finalM3u = new M3uFile();
                        finalM3u.parse(streamUm3u);
                        var allUrls = finalM3u.findAll(EntityType.INF).stream().
                                filter(a -> a.getAttributes().containsKey("channelUrl"))
                                .map(a -> a.getAttributes().get("channelUrl")).collect(Collectors.toList());
                        if (allUrls.size() > 100) {
                            var key = finalM3u.findAll(EntityType.KEY).stream().findFirst();
                            String keyFile = null;
                            String iv = null;
                            if (key.isPresent()) {
                                var method = key.get().getAttributes().get("METHOD");
                                var uri = key.get().getAttributes().get("URI");
                                iv = key.get().getAttributes().get("IV").replace("0x", "");
                                var target = in.getProtocol() + "://" + in.getHost() + uri;
                                var keyFileContent = getBinary(target);
                                keyFile = Hex.encodeHexString(keyFileContent);
                            }
                            var script = "";
                            var filelist = "";
                            var firstUrl = "";
                            for (int i = 0; i < allUrls.size(); i++) {
                                var url = allUrls.get(i);
                                if (i == 0) {
                                    firstUrl = url;
                                }
                                script += "curl --output " + padLeftZeros(Integer.toString(i), 4) + ".enc --url \"" + url + "\"\n";
                                if (keyFile != null) {
                                    script += "openssl aes-128-cbc -d -K " + keyFile + " -iv " + iv + " -in " + padLeftZeros(Integer.toString(i), 4) + ".enc -out " + padLeftZeros(Integer.toString(i), 4) + ".ts\n";
                                }
                                filelist += "file " + padLeftZeros(Integer.toString(i), 4) + ".ts\n";
                            }
                            File f = new File("test/"+episode);
                            if (!f.exists()) {
                                f.mkdirs();
                            }

                            try {
                                script+="\nmerge.bat";
                                Files.writeString(Path.of("test/"+episode, "download.bat"), script);
                                Files.writeString(Path.of("test/"+episode, "list.txt"), filelist);
                                Files.writeString(Path.of("test/"+episode, "merge.bat"), "ffmpeg -f concat -safe 0 -i list.txt -c copy \"" + episode + ".mp4\"");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println("result");
                        }
                    }
                }
                return false;

            }
        return false;
    }

    /**
     * Returns the class of the plugin settings.
     *
     * @return The settings class.
     */
    @Override
    public Class<?> getSettingClass() {
        return HttpFilterSettings.class;
    }

    /**
     * Returns the protocol phases this plugin handles.
     *
     * @return A list of protocol phases.
     */
    @Override
    public List<ProtocolPhase> getPhases() {
        return List.of(ProtocolPhase.POST_CALL, ProtocolPhase.PRE_CALL);
    }

    /**
     * Returns the unique identifier of the plugin.
     *
     * @return The plugin ID.
     */
    @Override
    public String getId() {
        return "sample-http";
    }

    /**
     * Returns the protocol this plugin supports.
     *
     * @return The protocol name.
     */
    @Override
    public String getProtocol() {
        return "http";
    }
}