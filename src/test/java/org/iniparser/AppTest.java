package org.iniparser;

import org.iniparser.Iniparser.IniFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Paths;

/**
 * Unit test for simple ini file de/encoding.
 */
public class AppTest {

    @Test
    public void shouldAnswerWithTrue() {
        System.out.println("Main test function running..");
        Iniparser iniparser = new Iniparser();

        // Define the test file path
        String filepath = "test-files/meow.ini";
        System.out.println("File to parse: " + filepath);
        Optional<IniFile> iniFile = Optional.empty();

        try {
            iniFile = iniparser.parse(filepath);
        } catch (IOException ex) {
            System.err.println("File parsing failed due to IO Exception:");
            ex.printStackTrace();
            fail("File parsing failed due to IOException: " + ex.getMessage());
            return;
        }

        // --- Assertions ---

        System.out.println("-- Test File parsed ---");

        // --- FOR DEBUGGING ---
        // System.out.println("--- START OF FILE CONTENT: " + filepath + " ---");
        // try {System.out.println(Files.readString(Paths.get(filepath))); } catch (IOException err) { return; }
        // System.out.println("--- END OF FILE CONTENT ---");
        // ---------------------------------

        assertTrue("Optional should contain an IniFile object.", iniFile.isPresent());
        IniFile config = iniFile.get();


        String result = config.getValue("author", "name");
        assertNotNull("Value should be retrieved successfully.", result);
        assertEquals("The 'name' key in the 'author' section must be 'meow'.", "meow", result);
        assertTrue(true);
    }
}
