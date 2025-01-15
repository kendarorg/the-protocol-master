package org.kendar.sample.m3u;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by tgermain on 30/12/2017.
 */
public class Parser {

    private static final String M3U_START_MARKER = "#EXTM3U";
    private static final String M3U_INFO_MARKER = "#EXTINF:";
    private static final Pattern DURATION_REGEX = Pattern.compile(".*#EXTINF:(.+?) .*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_ID_REGEX = Pattern.compile(".*tvg-id=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_NAME_REGEX = Pattern.compile(".*tvg-name=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_LOGO_REGEX = Pattern.compile(".*tvg-logo=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TVG_SHIFT_REGEX = Pattern.compile(".*tvg-shift=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_TITLE_REGEX = Pattern.compile(".*group-title=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RADIO_REGEX = Pattern.compile(".*radio=\"(.?|.+?)\".*", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHANNEL_NAME_REGEX = Pattern.compile(".*,(.+?)$", Pattern.CASE_INSENSITIVE);


    /**
     * Parse the m3u file
     *
     * @param stream pointing to your m3u file
     * @return
     */
    public List<Entry> parse(InputStream stream) {
        if (stream == null) {
            throw new ParsingException(0, "Cannot read stream");
        }
        List<Entry> entries = new ArrayList<>();
        int lineNbr = 0;
        String line;
        try {
            BufferedReader buffer = new BufferedReader(new InputStreamReader(stream));
            line = buffer.readLine();
            if(line == null) {
                throw new ParsingException(0, "Empty stream");
            }
            lineNbr++;
            checkStart(line);
            String globalTvgShif = extract(line, TVG_SHIFT_REGEX);

            Entry.Builder entry = null;
            while ((line = buffer.readLine()) != null) {
                lineNbr++;
                if (isExtInfo(line)) {
                    entry = extractExtInfo(globalTvgShif, line);
                } else {
                    if (entry == null) {
                        throw new ParsingException(lineNbr, "Missing " + M3U_INFO_MARKER);
                    }
                    entries.add(entry.channelUri(line).build());
                }
            }
        } catch (IOException e) {
            throw new ParsingException(lineNbr, "Cannot read file", e);
        }

        return entries;
    }

    private void checkStart(String line) {
        if (line != null) {
            if (!line.contains(M3U_START_MARKER)) {
                throw new ParsingException(1, "First line of the file should be " + M3U_START_MARKER);
            }
        }
    }

    private boolean isExtInfo(String line) {
        return line.contains(M3U_INFO_MARKER);
    }

    private Entry.Builder extractExtInfo(String globalTvgShift, String line) {
        String duration = extract(line, DURATION_REGEX);
        String tvgId = extract(line, TVG_ID_REGEX);
        String tvgName = extract(line, TVG_NAME_REGEX);
        String tvgShift = extract(line, TVG_SHIFT_REGEX);
        if (tvgShift == null) {
            tvgShift = globalTvgShift;
        }
        String radio = extract(line, RADIO_REGEX);
        String tvgLogo = extract(line, TVG_LOGO_REGEX);
        String groupTitle = extract(line, GROUP_TITLE_REGEX);
        String channelName = extract(line, CHANNEL_NAME_REGEX);

        return new Entry.Builder().channelName(channelName)
                .duration(duration)
                .groupTitle(groupTitle)
                .radio(radio)
                .tvgId(tvgId)
                .tvgLogo(tvgLogo)
                .tvgName(tvgName)
                .tvgShift(tvgShift);
    }

    private String extract(String line, Pattern pattern) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
}
