package org.iniparser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class Iniparser {

  // Better Practice: Composition (Recommended)
  public class IniFile {
      private final Map<String, Map<String, String>> sections;

      public IniFile(Map<String, Map<String, String>> sections) {
          this.sections = sections;
      }

      // Public getter methods
      public String getValue(String section, String key) {
          Map<String, String> sectionMap = sections.get(section);
          if (sectionMap != null) {
              return sectionMap.get(key);
          }
          return null; // Return null if section or key not found
      }
  }

  private Optional<String> src;
  private Integer cursor = 0;
  private Integer line   = 0;
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

  /**
   * Parses the INI file contents read from the given filepath.
   * @param filepath The path to the INI file.
   * @return An Optional containing the fully parsed IniFile object.
   * @throws IOException If the file cannot be read.
   */
  public Optional<IniFile> parse(String filepath) throws IOException {
    Path path         = Paths.get(filepath);
    String srcContent = Files.readString(path);

    this.src    = Optional.of(srcContent);
    this.cursor = 0;

    Map<String, Map<String, String>> iniMap = new HashMap<>();

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
          //this.cursor++; // Move past the ']' character
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
            readKVPair(iniMap);
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

    IniFile result = new IniFile(iniMap);
    return Optional.of(result);
  }

  public void readComment() {
    while (peekAhead() != '\n') {
      this.cursor++;
    }
  }

  public void readKVPair(Map<String, Map<String, String>> iniMap) {
    String key   = "";
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

    if (section != null) {
      section.put(key, value);
      iniMap.put(this.currentSection, section);
    }
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
    this.cursor         = 0;
    this.line           = 1;
    this.src            = Optional.empty();
    this.currentSection = "";
  }
}
