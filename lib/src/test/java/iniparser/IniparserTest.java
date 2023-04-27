package iniparser;

import org.junit.jupiter.api.Test;
import iniparser.Iniparser.IniMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IniparserTest {
    @Test void test() {
        Iniparser iniparser = new Iniparser();
        IniMap iniMap = iniparser.parse("test-files/meow.ini");
        System.out.println(iniMap.keySet());
        assertNotNull(iniMap);
        assertNotNull(iniMap.keySet());
        assertEquals(iniMap.getValue("general", "appname"), "configparser");
        assertEquals(iniMap.getValue("general", "version"), "0.1");
        assertEquals(iniMap.getValue("author", "name"), "meow");
        assertEquals(iniMap.getValue("author", "email"), "meow@gmail.com");
    }

    @Test void logOutput() {
        Iniparser iniparser = new Iniparser();
        IniMap iniMap = iniparser.parse("test-files/test2.ini");
        System.out.println(iniMap.keySet());
        System.out.println(iniMap.get("owner"));
        System.out.println(iniMap.get("database"));

        // @TODO: Handle comments ';' and '#'
        IniMap iniMap2 = iniparser.parse("test-files/test.ini");
        System.out.println(iniMap2.keySet());
        System.out.println(iniMap2.get("protocol"));
        System.out.println(iniMap2.get("user"));
    }
}
