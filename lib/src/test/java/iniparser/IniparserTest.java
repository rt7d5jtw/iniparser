package iniparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import iniparser.Iniparser.IniMap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IniparserTest {
    @Test void test() {
        Iniparser iniparser = new Iniparser();
        File fileToParse = new File("test-files/meow.ini");
        System.out.println(fileToParse);

        try { iniparser.setSource("test-files/meow.ini"); }
        catch (IOException ex) { return; };

        Optional<IniMap> iniMap = iniparser.parse("test-files/meow.ini");
        assertEquals(iniMap.isPresent(), true);

        System.out.println(iniMap.get().keySet());
        assertNotNull(iniMap);
        assertNotNull(iniMap.get().keySet());
        assertEquals(iniMap.get().getValue("general", "appname"), "configparser");
        assertEquals(iniMap.get().getValue("general", "version"), "0.1");
        assertEquals(iniMap.get().getValue("author", "name"), "meow");
        assertEquals(iniMap.get().getValue("author", "email"), "meow@gmail.com");
    }

    @Test void fileTest() {
        // Failure case
        Iniparser iniparser = new Iniparser();
        Exception exception = assertThrows(NoSuchFileException.class, () -> {
          iniparser.setSource("test-files/nofilehere.ini");
        });

        System.out.println("Exception message: " + exception.getMessage());
        //System.out.println("Exception cause: " + exception.getCause());
        //System.out.println("Exception stacktrace:\n");
        //exception.printStackTrace();
        assertTrue(exception.getMessage().contains("test-files/nofilehere.ini"));

        // Success case
        //iniparser.setSource("test-files/test.ini");
    }

    //@Test void logOutput() {
    //    Iniparser iniparser = new Iniparser();
    //    IniMap iniMap = iniparser.parse("test-files/test2.ini");
    //    System.out.println(iniMap.keySet());
    //    System.out.println(iniMap.get("owner"));
    //    System.out.println(iniMap.get("database"));

    //    // @TODO: Handle comments ';' and '#'
    //    IniMap iniMap2 = iniparser.parse("test-files/test.ini");
    //    System.out.println(iniMap2.keySet());
    //    System.out.println(iniMap2.get("protocol"));
    //    System.out.println(iniMap2.get("user"));
    //}
}
