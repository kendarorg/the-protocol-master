package org.kendar.sample.m3u;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uEntity {
    private static final Pattern DURATION_REGEX = Pattern.compile(".*#EXTINF:(.+?) .*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_ID_REGEX = Pattern.compile(".*tvg-id=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_NAME_REGEX = Pattern.compile(".*tvg-name=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_LOGO_REGEX = Pattern.compile(".*tvg-logo=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_SHIFT_REGEX = Pattern.compile(".*tvg-shift=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_TITLE_REGEX = Pattern.compile(".*group-title=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RADIO_REGEX = Pattern.compile(".*radio=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_NAME_REGEX = Pattern.compile(".*,(.+?)$", Pattern.CASE_INSENSITIVE);
    private final Map<String, String> attributes = new HashMap<String, String>();
    private EntityType type = EntityType.NONE;
    private String id;

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public EntityType getType() {
        return type;
    }

    void parse(String content) {
        content = content.trim();
        var data = content.split(":", 2);
        if (data.length == 1 && data[0].equalsIgnoreCase("#EXTM3U")) {
            type = EntityType.HEADER;
            id = data[0];
        } else if (data.length == 2 && data[0].equalsIgnoreCase("#EXTM3U")) {
            type = EntityType.HEADER;
            id = data[0];
            String globalTvgShif = extract(content, TVG_SHIFT_REGEX);
            if(globalTvgShif!=null) {
                attributes.put("tvgShift", globalTvgShif);
            }
        } else if (data.length == 2) {
            id = data[0];
            if (data[0].equalsIgnoreCase("#EXTINF")) {
                type = EntityType.INF;
                buildInfo(content);
            } else {
                if (data[0].equalsIgnoreCase("#EXT-X-MEDIA")) {
                    type = EntityType.MEDIA;
                    loadAttributes(data);
                } else if (data[0].equalsIgnoreCase("#EXT-X-STREAM-INF")) {
                    type = EntityType.STREAM_INF;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("##EXT-X-START")) {
                    type = EntityType.START;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("#EXT-X-MAP")) {
                    type = EntityType.MAP;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("#EXT-X-DATERANGE")) {
                    type = EntityType.DATE_RANGE;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("#EXT-X-SESSION-DATA")) {
                    type = EntityType.SESSION_DATA;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("#EXT-X-KEY")) {
                    type = EntityType.KEY;
                    loadAttributes(data);
                }else if (data[0].equalsIgnoreCase("#EXT-X-SESSION-KEY")) {
                    type = EntityType.SESSION_KEY;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-I-FRAMES-ONLY")) {
                    type = EntityType.IFRAMES_ONLY;
                }else if (data[0].equalsIgnoreCase("#EXT-X-PROGRAM-DATE-TIME")) {
                    type = EntityType.PROGRAM_DATE_TIME;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-GAP")) {
                    type = EntityType.GAP;
                }else if (data[0].equalsIgnoreCase("#EXT-X-DISCONTINUITY-SEQUENCE")) {
                    type = EntityType.DISCONTINUITY_SEQUENCE;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-ENDLIST")) {
                    type = EntityType.ENDLIST;
                }else if (data[0].equalsIgnoreCase("#EXT-X-DISCONTINUITY")) {
                    type = EntityType.DISCONTINUITY;
                }else if (data[0].equalsIgnoreCase("#EXT-X-BYTERANGE")) {
                    type = EntityType.BYTERANGE;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-MEDIA-SEQUENCE")) {
                    type = EntityType.MEDIA_SEQUENCE;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-VERSION")) {
                    type = EntityType.VERSION;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-TARGETDURATION")) {
                    type = EntityType.TARGETDURATION;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-PLAYLIST-TYPE")) {
                    type = EntityType.PLAYLIST_TYPE;
                    attributes.put("data",data[1]);
                }else if (data[0].equalsIgnoreCase("#EXT-X-INDEPENDENT-SEGMENTS")) {
                    type = EntityType.INDEPENDENT_SEGMENTS;
                }else{
                    type = EntityType.UNKNOW;
                }
            }
        }
    }

    public int getBandwith(){
        if(getAttributes().containsKey("BANDWIDTH")){
            return Integer.parseInt(getAttributes().get("BANDWIDTH"));
        }
        return 0;
    }

    private void loadAttributes(String[] data) {
        var inString = false;
        var prevline = "";
        var values = new ArrayList<String>();
        for(var letter:data[1].toCharArray()) {
            if(!inString && letter==',') {
                values.add(prevline);
                prevline = "";
            }else if(!inString && letter=='"') {
                inString=true;
                prevline+=letter;
            }else if(inString && letter=='"') {
                inString=false;
                prevline+=letter;
            }else{
                prevline+=letter;
            }
        }
        if(!prevline.isEmpty()) {
            values.add(prevline);
        }

        for (var value : values) {
            var splValue = value.split("=", 2);
            if(splValue.length==2) {
                if (splValue[1].startsWith("\"") && splValue[1].endsWith("\"")) {
                    splValue[1] = splValue[1].substring(1, splValue[1].length() - 1);
                }
                attributes.put(splValue[0], splValue[1]);
            }else{
                System.out.println(splValue);
            }
        }
    }

    private String extract(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private void buildInfo(String line) {
        attributes.put("duration", extract(line, DURATION_REGEX));
        attributes.put("tvgId", extract(line, TVG_ID_REGEX));
        attributes.put("tvgName", extract(line, TVG_NAME_REGEX));
        attributes.put("tvgShift", extract(line, TVG_SHIFT_REGEX));

        attributes.put("radio", extract(line, RADIO_REGEX));
        attributes.put("tvgLogo", extract(line, TVG_LOGO_REGEX));
        attributes.put("groupTitle", extract(line, GROUP_TITLE_REGEX));
        attributes.put("channelName", extract(line, CHANNEL_NAME_REGEX));
    }
}
