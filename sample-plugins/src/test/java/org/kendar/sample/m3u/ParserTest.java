package org.kendar.sample.m3u;


import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by tgermain on 30/12/2017.
 */
public class ParserTest {

    @Test
    public void testParse() throws Exception {
        List<Entry> entries = new Parser().parse(ParserTest.class.getResourceAsStream("/test.m3u"));

        assertEquals( 3, entries.size());

        assertEquals("La Une HD", entries.get(0).getChannelName());
        assertEquals("La Une HD", entries.get(0).getTvgName());
        assertEquals("La_Une", entries.get(0).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/84.ts", entries.get(0).getChannelUri());
        assertEquals("-1", entries.get(0).getDuration());
        assertEquals("TV", entries.get(0).getGroupTitle());
        assertEquals("La Une HD", entries.get(0).getChannelName());
        assertEquals("false", entries.get(0).getRadio());
        assertEquals("../logo/La_Une.png", entries.get(0).getTvgLogo());
        assertEquals("1", entries.get(0).getTvgShift());

        assertEquals("La Deux HD", entries.get(1).getChannelName());
        assertEquals("La Deux HD", entries.get(1).getTvgName());
        assertEquals("La_Deux", entries.get(1).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/85.ts", entries.get(1).getChannelUri());
        assertEquals("-1", entries.get(1).getDuration());
        assertEquals("TV", entries.get(1).getGroupTitle());
        assertEquals("La Deux HD", entries.get(1).getChannelName());
        assertEquals("false", entries.get(1).getRadio());
        assertEquals("../logo/La_Deux.png", entries.get(1).getTvgLogo());
        assertEquals("2", entries.get(1).getTvgShift());

        assertEquals("RTL TVI HD", entries.get(2).getChannelName());
        assertEquals("RTL TVI HD", entries.get(2).getTvgName());
        assertEquals("RTL_TVI", entries.get(2).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/719.ts", entries.get(2).getChannelUri());
        assertEquals("-1", entries.get(2).getDuration());
        assertEquals("TV", entries.get(2).getGroupTitle());
        assertEquals("RTL TVI HD", entries.get(2).getChannelName());
        assertEquals("false", entries.get(2).getRadio());
        assertEquals("../logo/RTL_TVI.png", entries.get(2).getTvgLogo());
        assertEquals("3", entries.get(2).getTvgShift());
    }

    @Test
    public void testParseGlobalTvgShift() {
        List<Entry> entries = new Parser().parse(ParserTest.class.getResourceAsStream("/test_global_tvg_shift.m3u"));

        assertEquals( 3, entries.size());

        assertEquals("La Une HD", entries.get(0).getChannelName());
        assertEquals("La Une HD", entries.get(0).getTvgName());
        assertEquals("La_Une", entries.get(0).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/84.ts", entries.get(0).getChannelUri());
        assertEquals("-1", entries.get(0).getDuration());
        assertEquals("TV", entries.get(0).getGroupTitle());
        assertEquals("La Une HD", entries.get(0).getChannelName());
        assertEquals("false", entries.get(0).getRadio());
        assertEquals("../logo/La_Une.png", entries.get(0).getTvgLogo());
        assertEquals("4", entries.get(0).getTvgShift());

        assertEquals("La Deux HD", entries.get(1).getChannelName());
        assertEquals("La Deux HD", entries.get(1).getTvgName());
        assertEquals("La_Deux", entries.get(1).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/85.ts", entries.get(1).getChannelUri());
        assertEquals("-1", entries.get(1).getDuration());
        assertEquals("TV", entries.get(1).getGroupTitle());
        assertEquals("La Deux HD", entries.get(1).getChannelName());
        assertEquals("false", entries.get(1).getRadio());
        assertEquals("../logo/La_Deux.png", entries.get(1).getTvgLogo());
        assertEquals("4", entries.get(1).getTvgShift());

        assertEquals("RTL TVI HD", entries.get(2).getChannelName());
        assertEquals("RTL TVI HD", entries.get(2).getTvgName());
        assertEquals("RTL_TVI", entries.get(2).getTvgId());
        assertEquals("http://panel.globepremium.com:8000/live/xxx/yyy/719.ts", entries.get(2).getChannelUri());
        assertEquals("-1", entries.get(2).getDuration());
        assertEquals("TV", entries.get(2).getGroupTitle());
        assertEquals("RTL TVI HD", entries.get(2).getChannelName());
        assertEquals("false", entries.get(2).getRadio());
        assertEquals("../logo/RTL_TVI.png", entries.get(2).getTvgLogo());
        assertEquals("4", entries.get(2).getTvgShift());
    }

    @Test
    public void testParseBigFile() {
        List<Entry> entries = new Parser().parse(ParserTest.class.getResourceAsStream("/test_big_file.m3u"));
        assertEquals(9065, entries.size());
    }

    @Test
    public void testEmptyStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[]{});
        assertThrows(ParsingException.class,()->new Parser().parse(bais));
    }

    @Test
    public void testWrongStream() throws IOException {
        InputStream stream = ParserTest.class.getResourceAsStream("/test_global_tvg_shift.m3u");
        stream.close();
        assertThrows(ParsingException.class,()->new Parser().parse(stream));
    }
}


