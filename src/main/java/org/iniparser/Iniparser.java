package org.iniparser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.io.InputStream;

//public class App 
//{
//    public static void main( String[] args )
//    {
//        System.out.println( "Hello World!" );
//    }
//}
public class Iniparser {

  /**
   * HashMap that holds a HashMap to store each section of
   * the ini-file in a HashMap.
   */
  class IniMap extends HashMap<String, Map<String, String>> {

    private static final long serialVersionUID = 1L;

    public IniMap() {
      super();
    }

    /**
     * Get value by key from a section.
     */
    String getValue(String section, String key) {
      return IniMap.super.get(section).get(key);
    }
  };

  private Optional<IniMap> iniMap = Optional.empty();
  private Optional<String> src;
  private Integer cursor = 0;
  private Integer line = 0;
  private String currentSection;

  /**
   * Paths: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Paths.html
   * Files: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/Files.html
   * @throws IOException
   * */
  public void setSource(String filepath) throws IOException {
    // Resolved system dependent filepath
    Path path = Paths.get(filepath);
    src = Optional.of(Files.readString(path));
  }

  public Optional<IniMap> parse(String filepath) {
    if (src.isEmpty()) { // If source has not been set, return empty map
      return iniMap;
    }

    // Initialize Map for the inifile
    iniMap = Optional.of(new IniMap());

    while (!isAtEOF()) {
      switch (peek()) {
        case '\n' -> {
          this.line++;
          this.cursor++;
          break;
        }
        case '[' -> {
          this.cursor++;
          String section = "";
          while (peek() != ']') {
            section += peek();
            this.cursor++;
          }

          Map<String, String> sectionMap = new HashMap<>();
          iniMap.get().put(section, sectionMap);

          //System.out.println("Section found! " + section);
          // Mark this as the current section for key-value parsing
          this.currentSection = section;
          break;
        }
        case ';', '#' -> {
          //System.out.println(peek());
          readComment();
          this.cursor++; // Move cursor out of the comment
          break;
        }
        default -> {
          if (Character.isAlphabetic(peek())) {
            readKVPair();
          }

          if (Character.isDigit(peek())) {
            // System.out.println(peek());
          }

          this.cursor++;
        }
      }
    }

    // Reset the state of the parser
    this.reset();

    return iniMap;
  }

  public void readComment() {
    while (peekAhead() != '\n') {
      this.cursor++;
    }
  }

  public void readKVPair() {
    String key = "";
    String value = "";

    while (peek() != '\n') {
      // Read the key
      while (peek() != '=') {
        if (!Character.isWhitespace(peek())) {
          key += peek();
        }
        this.cursor++;
      }

      // Read the value
      if (peek() == '=') {
        this.cursor++; // Move cursor from `=`
        while (peek() != '\n') {
          // Skip comments
          if (peek() == ';' || peek() == '#') {
            readComment();
            // Move away from them while loop without touching the cursor
            break; 
          } else {
            value += peek();
          }

          this.cursor++;
        }

        // Move away from the while loop without touching the cursor!
        break;
      }

      // Move cursor to newline
      this.cursor++;
    }

    // Trim all the whitespace from around the value (leading and trailing whitespaces)
    value = value.trim();

    // Get the current map of the section
    Map<String, String> section = iniMap.get().get(this.currentSection);
    section.put(key, value);
    iniMap.get().put(this.currentSection, section);
  }

  public char peekAhead() {
    return this.src.get().charAt(this.cursor + 1);
  }

  public char peek() {
    return this.src.get().charAt(this.cursor);
  }

  public boolean isAtEOF() {
    return this.cursor >= this.src.get().length();
  }

  /**
   * Resets cursor, line, src and currentSection for next parse.
   */
  public void reset() {
    this.cursor = 0;
    this.line = 1;
    this.src = Optional.empty();
    this.currentSection = "";
  }
}
