package org.kendar.sample.m3u;


import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by tgermain on 30/12/2017.
 */
public class WriterTest {

    @Test
    public void testWrite() {
        List<Entry> entries = new Parser().parse(WriterTest.class.getResourceAsStream("/test.m3u"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Writer().write(entries, baos);
        assertEquals(getOrigFile(), new String(baos.toByteArray()));
    }

    @Test
    public void testWriteSingleQuote() {
        Entry entry = new Entry.Builder()
                .channelUri("http://test.com")
                .tvgName("test'ing")
                .channelName("test-channel'name")
                .groupTitle("TV'Group")
                .duration("-1")
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Writer().write(Collections.singletonList(entry), baos);
        assertEquals("#EXTM3U" + System.lineSeparator() +
                "#EXTINF:-1 tvg-name=\"test'ing\" group-title=\"TV'Group\",test-channel'name" + System.lineSeparator() +
                "http://test.com", new String(baos.toByteArray()));
    }

    private String getOrigFile() {
        return new BufferedReader(new InputStreamReader(WriterTest.class.getResourceAsStream("/test.m3u")))
                .lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
