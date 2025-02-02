/*
 * Copyright 2005,2009 Ivan SZKIBA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ini4j;

import org.ini4j.sample.Dwarfs;

import org.ini4j.test.DwarfsData;
import org.ini4j.test.Helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.io.*;

public class IniTest extends Ini4jCase {
    private static final String COMMENT_ONLY = "# first line\n# second line\n";
    private static final String COMMENT_ONLY_VALUE = " first line\n second line";
    private static final String INI_ONE_HEADER = COMMENT_ONLY + "\n\n[section]\nkey=value\n";
    private static final String COMMENTED_OPTION = COMMENT_ONLY + "\n\n[section]\n;comment\nkey=value\n";
    private static final String MULTI = "[section]\noption=value\noption=value2\n[section]\noption=value3\noption=value4\noption=value5\n";
    private static final String DOS_PATH = "src/test/resources/org/ini4j/addon/dos.ini";
    @Test
    public void testCommentedOption() throws Exception {
        Ini ini = new Ini(new StringReader(COMMENTED_OPTION));

        assertEquals("comment", ini.get("section").getComment("key"));
    }

    @Test
    public void testCommentOnly() throws Exception {
        Ini ini = new Ini(new StringReader(COMMENT_ONLY));

        assertEquals(COMMENT_ONLY_VALUE, ini.getComment());
    }

    @Test
    public void testLoad() throws Exception {
        Ini ini;

        ini = new Ini(Helper.getResourceURL(Helper.DWARFS_INI));
        Helper.assertEquals(DwarfsData.dwarfs, ini.as(Dwarfs.class));
        ini = new Ini(Helper.getResourceStream(Helper.DWARFS_INI));
        Helper.assertEquals(DwarfsData.dwarfs, ini.as(Dwarfs.class));
        ini = new Ini(Helper.getResourceReader(Helper.DWARFS_INI));
        Helper.assertEquals(DwarfsData.dwarfs, ini.as(Dwarfs.class));
        ini = new Ini(Helper.getSourceFile(Helper.DWARFS_INI));
        Helper.assertEquals(DwarfsData.dwarfs, ini.as(Dwarfs.class));
        ini = new Ini();
        ini.setFile(Helper.getSourceFile(Helper.DWARFS_INI));
        ini.load();
        Helper.assertEquals(DwarfsData.dwarfs, ini.as(Dwarfs.class));
    }

    @Test
    public void testLoadException() throws Exception {
        Ini ini = new Ini();

        try {
            ini.load();
            missing(FileNotFoundException.class);
        } catch (FileNotFoundException x) {
            //
        }
    }

    @Test
    public void testMulti() throws Exception {
        Ini ini = new Ini(new StringReader(MULTI));
        Ini.Section sec;

        assertEquals(1, ini.length("section"));
        assertEquals(5, ini.get("section", 0).length("option"));
        ini.clear();
        Config cfg = new Config();

        cfg.setMultiSection(true);
        ini.setConfig(cfg);
        ini.load(new StringReader(MULTI));
        assertEquals(2, ini.get("section", 0).length("option"));
        assertEquals(3, ini.get("section", 1).length("option"));

        //
        StringWriter writer = new StringWriter();

        cfg.setMultiOption(false);
        ini.store(writer);
        ini.clear();
        cfg.setMultiOption(true);
        ini.load(new StringReader(writer.toString()));
        assertEquals(1, ini.get("section", 0).length("option"));
        assertEquals(1, ini.get("section", 1).length("option"));
        assertEquals("value2", ini.get("section", 0).get("option"));
        assertEquals("value5", ini.get("section", 1).get("option"));

        //
        ini.clear();
        cfg.setMultiOption(false);
        ini.load(new StringReader(MULTI));
        assertEquals(1, ini.get("section", 0).length("option"));
        assertEquals(1, ini.get("section", 1).length("option"));
    }

    @Test
    public void testOneHeaderOnly() throws Exception {
        Ini ini = new Ini(new StringReader(INI_ONE_HEADER));

        assertEquals(COMMENT_ONLY_VALUE, ini.getComment());
    }

    @Test
    public void testStore() throws Exception {
        Ini ini = Helper.newDwarfsIni();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        ini.store(buffer);
        Ini dup = new Ini();

        dup.load(new ByteArrayInputStream(buffer.toByteArray()));
        Helper.assertEquals(DwarfsData.dwarfs, dup.as(Dwarfs.class));
        buffer = new ByteArrayOutputStream();
        ini.store(new OutputStreamWriter(buffer));
        dup = new Ini();
        dup.load(new InputStreamReader(new ByteArrayInputStream(buffer.toByteArray())));
        Helper.assertEquals(DwarfsData.dwarfs, dup.as(Dwarfs.class));

        //
        File file = File.createTempFile("test", ".ini");

        file.deleteOnExit();
        ini.setFile(file);
        assertEquals(file, ini.getFile());
        ini.store();
        dup = new Ini();
        dup.setFile(file);
        dup.load();
        Helper.assertEquals(DwarfsData.dwarfs, dup.as(Dwarfs.class));
        file.delete();
    }

    @Test
    public void testStoreException() throws Exception {
        Ini ini = new Ini();

        try {
            ini.store();
            missing(FileNotFoundException.class);
        } catch (FileNotFoundException x) {
            //
        }
    }

    @Test
    public void testWithComment() throws Exception {
        Ini ini = new Ini();

        ini.load(Helper.getResourceStream(Helper.DWARFS_INI));
        assertNotNull(ini.getComment());
        for (Ini.Section sec : ini.values()) {
            assertNotNull(ini.getComment(sec.getName()));
        }
    }

    @Test
    public void testWithoutComment() throws Exception {
        Ini ini = new Ini();
        Config cfg = new Config();

        cfg.setComment(false);
        ini.setConfig(cfg);
        ini.load(Helper.getResourceStream(Helper.DWARFS_INI));
        assertNull(ini.getComment());
        for (Ini.Section sec : ini.values()) {
            assertNull(ini.getComment(sec.getName()));
        }

        ini = new Ini();
        ini.setConfig(cfg);
        ini.setComment("comment");
        Ini.Section sec = ini.add("section");

        sec.add("option", "value");
        ini.putComment("section", "section-comment");
        StringWriter writer = new StringWriter();

        ini.store(writer);
        assertEquals("[section]\noption = value\n\n", writer.toString());
    }

    @Test
    public void testWithoutHeaderComment() throws Exception {
        Ini ini = new Ini();
        Config cfg = new Config();

        cfg.setHeaderComment(false);
        cfg.setComment(true);
        ini.setConfig(cfg);
        ini.load(Helper.getResourceStream(Helper.DWARFS_INI));
        assertNull(ini.getComment());
        for (Ini.Section sec : ini.values()) {
            assertNotNull(ini.getComment(sec.getName()));
        }

        ini = new Ini();
        ini.setConfig(cfg);
        ini.setComment("comment");
        Ini.Section sec = ini.add("section");

        sec.add("option", "value");
        ini.putComment("section", "section-comment");
        StringWriter writer = new StringWriter();

        ini.store(writer);
        assertEquals("#section-comment\n[section]\noption = value\n\n", writer.toString());
    }

    @Test
    public void testCVE_2022_41404() throws Exception {
        Ini ini = new Ini();
        ini.load(new File(DOS_PATH));
        try {
            ini.get("deploy").fetch("a");
            missing(IllegalArgumentException.class);
        } catch (IllegalArgumentException x) {
            //
        }
    }
}
