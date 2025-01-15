package org.kendar.sample.m3u;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.List;

/**
 * Created by Thomas on 30/12/2017.
 */
public class Writer {

    private static final String M3U_START_MARKER = "#EXTM3U";
    private static final String M3U_INFO_MARKER = "#EXTINF:";
    private static final String DURATION_PATTERN ="{0} ";
    private static final String TVG_ID_PATTERN ="tvg-id=\"{0}\" ";
    private static final String TVG_NAME_PATTERN ="tvg-name=\"{0}\" ";
    private static final String TVG_LOGO_PATTERN ="tvg-logo=\"{0}\" ";
    private static final String TVG_SHIFT_PATTERN ="tvg-shift=\"{0}\" ";
    private static final String GROUP_TITLE_PATTERN ="group-title=\"{0}\" ";
    private static final String RADIO_PATTERN ="radio=\"{0}\" ";
    private static final String CHANNEL_NAME_PATTERN =",{0}";

    public void write(List<Entry> entries, OutputStream stream) {
        PrintWriter pw = new PrintWriter(stream);

        pw.println(M3U_START_MARKER);

        StringBuilder builder;
        for (Entry entry : entries) {
            builder = new StringBuilder();
            builder.append(M3U_INFO_MARKER);
            appendIfNotNull(builder, entry.getDuration(), DURATION_PATTERN);
            appendIfNotNull(builder, entry.getTvgId(), TVG_ID_PATTERN);
            appendIfNotNull(builder, entry.getTvgName(), TVG_NAME_PATTERN);
            appendIfNotNull(builder, entry.getTvgShift(), TVG_SHIFT_PATTERN);
            appendIfNotNull(builder, entry.getRadio(), RADIO_PATTERN);
            appendIfNotNull(builder, entry.getTvgLogo(), TVG_LOGO_PATTERN);
            appendIfNotNull(builder, entry.getGroupTitle(), GROUP_TITLE_PATTERN);
            builder.deleteCharAt(builder.length()-1);
            appendIfNotNull(builder, entry.getChannelName(), CHANNEL_NAME_PATTERN);
            builder.append(System.lineSeparator());
            builder.append(entry.getChannelUri());
            if(entries.indexOf(entry) != entries.size() -1) {
                builder.append(System.lineSeparator());
            }
            pw.print(builder.toString());
            pw.flush();
        }
        pw.flush();
    }

    private void appendIfNotNull(StringBuilder sb, String toWrite, String pattern) {
        if(toWrite != null) {
            sb.append(MessageFormat.format(pattern, toWrite));
        }
    }
}
