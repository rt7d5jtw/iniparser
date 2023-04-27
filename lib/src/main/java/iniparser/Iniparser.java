package iniparser;

import java.util.HashMap;
import java.util.Map;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.io.InputStream;

public class Iniparser {

  /**
   * HashMap that holds a HashMap to store each section of
   * the ini-file in a HashMap.
   */
  class IniMap extends HashMap<String, Map<String, String>> {
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

  private IniMap iniMap = new IniMap();
  private String src = "";
  private Integer cursor = 0;
  private Integer line = 0;
  private String currentSection;

  public IniMap parse(String source) {
    Path filepath = Paths.get(source);
    try {
      src = Files.readString(filepath);
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
            iniMap.put(section, sectionMap);

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
    } catch (IOException ex) {
      ex.printStackTrace();
    }

    // Reset state of the parser
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
    Map<String, String> section = iniMap.get(this.currentSection);
    section.put(key, value);
    iniMap.put(this.currentSection, section);
  }

  public char peekAhead() {
    return this.src.charAt(this.cursor + 1);
  }

  public char peek() {
    return this.src.charAt(this.cursor);
  }

  public boolean isAtEOF() {
    return this.cursor >= this.src.length();
  }

  /**
   * Resets cursor, line, src and currentSection for next parse.
   */
  public void reset() {
    this.cursor = 0;
    this.line = 1;
    this.src = "";
    this.currentSection = "";
  }

  public InputStream loadFileFromResources(final String fileName) {
    return this.getClass()
        .getClassLoader()
        .getResourceAsStream(fileName);
  }
}
