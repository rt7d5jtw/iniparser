package org.iniparser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class Iniparser {

  public class IniFile {
    private final Map<String, Map<String, String>> sections;

    public IniFile(Map<String, Map<String, String>> sections) {
      this.sections = sections;
    }

    public String getValue(String section, String key) {
      return sections.getOrDefault(section, Collections.emptyMap()).get(key);
    }
  }

  private Optional<String> src = Optional.empty();
  private int cursor = 0;
  private int line   = 0;
  private String currentSection;

  /**
   * Parses the INI file contents read from the given filepath.
   * @param filepath The path to the INI file.
   * @return An Optional containing the fully parsed IniFile object.
   * @throws IOException If the file cannot be read.
   */
  public Optional<IniFile> parse(String filepath) throws IOException {
    Path path    = Paths.get(filepath);
    String fileSrc = Files.readString(path);

    this.src    = Optional.of(fileSrc);
    this.cursor = 0;

    Map<String, Map<String, String>> iniMap = new HashMap<>();

    while (!isAtEOF()) {
      switch (peek()) {
        case '\n' -> {
          this.line++;
          this.cursor++;
        }
        case '[' -> {
          this.cursor++;
          StringBuilder sectionBuilder = new StringBuilder();

          while (!isAtEOF() && peek() != ']') {
            sectionBuilder.append(peek());
            this.cursor++;
          }

          String section = sectionBuilder.toString();
          Map<String, String> sectionMap = new HashMap<>();
          iniMap.put(section, sectionMap);

          this.currentSection = section;

          if (!isAtEOF()) this.cursor++; // Move past the ']' character safely
        }
        case ';', '#' -> {
          readComment();
          if (!isAtEOF()) this.cursor++; // Move cursor out of the comment safely
        }
        default -> {
          if (Character.isAlphabetic(peek())) {
            readKVPair(iniMap);
          } else {
            this.cursor++; // Ensure we always advance to avoid infinite loops
          }
        }
      }
    }

    // Reset the state of the parser
    this.reset();

    IniFile result = new IniFile(iniMap);
    return Optional.of(result);
  }

  public void readComment() {
    while (!isAtEOF() && peekAhead() != '\n' && peekAhead() != '\0') {
      this.cursor++;
    }
  }

  public void readKVPair(Map<String, Map<String, String>> iniMap) {
    StringBuilder keyBuilder = new StringBuilder();
    StringBuilder valueBuilder = new StringBuilder();

    while (!isAtEOF() && peek() != '\n') {
      // Read the key
      while (!isAtEOF() && peek() != '=') {
        if (!Character.isWhitespace(peek())) {
          keyBuilder.append(peek());
        }
        this.cursor++;
      }

      // Read the value
      if (!isAtEOF() && peek() == '=') {
        this.cursor++; // Move cursor from `=`
        while (!isAtEOF() && peek() != '\n') {
          // Skip comments
          if (peek() == ';' || peek() == '#') {
            readComment();
            break;
          } else {
            valueBuilder.append(peek());
          }
          this.cursor++;
        }
        break;
      }

      if (!isAtEOF()) { this.cursor++; }
    }

    String key = keyBuilder.toString();
    String value = valueBuilder.toString().trim();

    // Safely insert into the map
    if (this.currentSection != null && iniMap.containsKey(this.currentSection)) {
      Map<String, String> section = iniMap.get(this.currentSection);
      section.put(key, value);
    }
  }

  public char peekAhead() {
    if (this.cursor + 1 >= this.src.get().length()) { return '\0'; } // Prevents out-of-bounds on last char
    return this.src.get().charAt(this.cursor + 1);
  }

  public char peek() {
    if (isAtEOF()) { return '\0'; }
    return this.src.get().charAt(this.cursor);
  }

  public boolean isAtEOF() {
    return !this.src.isPresent() || this.cursor >= this.src.get().length();
  }

  /**
   * Resets cursor, line, src and currentSection for next parse.
   */
  public void reset() {
    this.cursor         = 0;
    this.line           = 1;
    this.src            = Optional.empty();
    this.currentSection = null;
  }
}
