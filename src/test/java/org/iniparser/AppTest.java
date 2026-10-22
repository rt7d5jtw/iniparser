package org.iniparser;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Unit tests for simple ini file parsing.
 * Made to be Java 3 and JUnit 3.8.1 compatible.
 * NOTE: In JUnit 3, tests are identified by the "test" method prefix
 */
public class AppTest extends TestCase {

    private Iniparser iniparser;

    // Overrides JUnit 3's setup method
    protected void setUp() throws Exception {
        super.setUp();
        iniparser = new Iniparser();
    }

    /**
     * Helper method to parse files and automatically fail the test if an IOException occurs.
     */
    private IniFile safeParse(String filepath) {
        try {
            System.out.println("Parsing file: " + filepath);
            return iniparser.parse(filepath);
        } catch (IOException ex) {
            fail("File parsing failed due to IOException: " + ex.getMessage());
            return null;
        }
    }

    private IniFile safeParse(String filepath, String encoding) {
        try {
            System.out.println("Parsing file: " + filepath);
            return iniparser.parse(filepath, encoding);
        } catch (IOException ex) {
            fail("File parsing failed due to IOException: " + ex.getMessage());
            return null;
        }
    }

    public void testParseStandardIni() {
        IniFile inifile = safeParse("test-files/test.ini");
        assertNotNull("IniFile should not be null", inifile);

        assertEquals("6", inifile.getValue("protocol", "version"));
        
        // Tests space stripping around '=' and ignoring inline comments
        assertEquals("Bob Smith",     inifile.getValue("user", "name"));
        assertEquals("bob@smith.com", inifile.getValue("user", "email"));
        assertEquals("true",          inifile.getValue("user", "active"));
        assertEquals("3.14159",       inifile.getValue("user", "pi"));
        
        assertEquals("John L. Ingraham", inifile.getValue("author", "name"));
        assertEquals("March of the Microbes: Sighting the Unseen", inifile.getValue("author", "book"));
    }

    public void testParseMeowIni() {
        IniFile inifile = safeParse("test-files/meow.ini");
        assertNotNull(inifile);

        assertEquals("configparser", inifile.getValue("general", "appname"));
        assertEquals("0.1", inifile.getValue("general", "version"));
        
        assertEquals("meow", inifile.getValue("author", "name"));
        assertEquals("meow@gmail.com", inifile.getValue("author", "email"));
        
        assertEquals("mike", inifile.getValue("tasks", "student"));
    }

    public void testParseTest2Ini() {
        IniFile inifile = safeParse("test-files/test2.ini");
        assertNotNull(inifile);

        assertEquals("wikipedia.org", inifile.getValue("section", "domain"));
        assertEquals("bar", inifile.getValue(".subsection", "foo"));
        
        assertEquals("John Doe", inifile.getValue("owner", "name"));
        assertEquals("Acme Widgets Inc.", inifile.getValue("owner", "organization"));
        
        assertEquals("192.0.2.62", inifile.getValue("database", "server"));
        assertEquals("143", inifile.getValue("database", "port"));
        assertEquals("\"payroll.dat\"", inifile.getValue("database", "file"));
    }

    public void testSortingStressIni() {
        IniFile inifile = safeParse("test-files/sorting_stress.ini");
        assertNotNull(inifile);

        // Tests non-consecutive sections grouping correctly
        assertEquals("42", inifile.getValue("zebra", "stripes"));
        assertEquals("savannah", inifile.getValue("zebra", "habitat"));
        
        // Tests alphabetization of keys
        assertEquals("first", inifile.getValue("alpha", "alpha"));
        assertEquals("last",  inifile.getValue("alpha", "zeta"));
        assertEquals("third", inifile.getValue("alpha", "gamma"));
        
        assertEquals("30", inifile.getValue("network", "timeout"));
    }

    public void testEditorConfigIni() {
        IniFile inifile = safeParse("test-files/editorconfig.ini");
        assertNotNull(inifile);

        // Checking complex section headers and glob patterns
        assertEquals("utf-8", inifile.getValue("*", "charset"));
        assertEquals("lf", inifile.getValue("*", "end_of_line"));
        
        assertEquals("space", inifile.getValue("*.{py,pyw}", "indent_style"));
        assertEquals("4", inifile.getValue("*.{py,pyw}", "indent_size"));
        
        assertEquals("tab", inifile.getValue("Makefile", "indent_style"));
        
        // Checking inline comment parsing edge cases
        assertEquals("false", inifile.getValue("*.md", "trim_trailing_whitespace"));
        assertEquals("80", inifile.getValue("*.md", "max_line_length"));
    }

    public void testMultilineAndEdgeCasesIni() {
        IniFile inifile = safeParse("test-files/multiline_and_edge_cases.ini");
        assertNotNull(inifile);

        assertEquals("worker-daemon", inifile.getValue("service", "name"));
        assertEquals("4", inifile.getValue("service", "threads"));
        
        assertEquals("true", inifile.getValue("flags", "debug_mode"));
        assertEquals("false", inifile.getValue("flags", "enable_telemetry"));
        
        // Tests comma separated lists
        assertEquals("http://localhost, http://127.0.0.1", inifile.getValue("flags", "allowed_origins"));

        assertEquals("/healthz", inifile.getValue("endpoints", "health_check"));
        assertEquals("/api/v1", inifile.getValue("endpoints", "api_v1"));
    }

    /**
     * Tests parsing a UTF-16 LE file that begins with a BOM (FF FE).
     *
     * A parser that reads the file as Latin-1/UTF-8 will see the BOM as
     * garbage and every following ASCII character as NUL-separated bytes,
     * so the very first section header will look like "\uFEFF[general]"
     * (or worse) and lookups by the plain name will fail.
     */
    public void testParseUtf16LeBomIni() {
        IniFile inifile = safeParse("test-files/bom_utf16le.ini");
        assertNotNull("IniFile should not be null", inifile);

        // If the BOM leaks into the first section name, this returns null.
        assertEquals("Bom Test", inifile.getValue("general", "name"));
        assertEquals("1.0",      inifile.getValue("general", "version"));

        // Sections appended after the original content (CRLF line endings)
        assertEquals("localhost", inifile.getValue("database", "host"));
        assertEquals("5432",      inifile.getValue("database", "port"));

        // Backslashes in values (Java string "C:\\Users\\test" is C:\Users\test)
        assertEquals("C:\\Users\\test", inifile.getValue("paths", "home"));

        // Non-ASCII characters survived the UTF-16LE decoding: "Caf\u00e9 \u20ac" is "Café €"
        assertEquals("Caf\u00e9 \u20ac", inifile.getValue("paths", "name"));

        // Empty section must not produce keys
        assertNull("Empty section should have no keys", inifile.getValue("empty", "key"));

        // Whitespace around '=' and the trailing space should be trimmed
        assertEquals("value with spaces", inifile.getValue("last", "key"));

        // The comment line must not be parsed as a key
        assertNull("Comment should not become a key", inifile.getValue("database", "; comment line"));
    }

    /**
     * Same as above but for UTF-16 BE with BOM (FE FF).
     * The two files share identical logical content, so the assertions
     * are deliberately parallel — a parser that hard-codes one
     * endianness will pass one test and fail the other.
     */
    public void testParseUtf16BeBomIni() {
        IniFile inifile = safeParse("test-files/bom_utf16be.ini");
        assertNotNull("IniFile should not be null", inifile);

        assertEquals("Bom Test", inifile.getValue("general", "name"));
        assertEquals("1.0",      inifile.getValue("general", "version"));
    }

    public void testParseUtf8BomIni() {
        IniFile inifile = safeParse("test-files/bom_utf8.ini");
        assertNotNull("IniFile should not be null", inifile);
        assertEquals("localhost", inifile.getValue("db", "host"));
    }

    public void testParseCP1252() {
        String encoding = "Cp1252";
        IniFile inifile = safeParse("test-files/francais.ini", encoding);
        assertNotNull("IniFile should not be null", inifile);

        String value1 = inifile.getValue("Paramètres", "ville");
        assertEquals("Besançon", value1);

        String value2 = inifile.getValue("Paramètres", "prix");
        assertEquals("12 €", value2);

        String value3 = inifile.getValue("Paramètres", "citation");
        assertEquals("“Bonjour”", value3);
    }
}
