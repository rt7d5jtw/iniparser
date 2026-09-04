package org.iniparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

public class Iniparser {
  private String src = null;
  private int cursor = 0;
  private int line   = 0;
  private String currentSection;

  /**
   * Parses the INI file contents read from the given filepath.
   * @param filepath The path to the INI file.
   * @return An Optional containing the fully parsed IniFile object.
   * @throws IOException If the file cannot be read.
   */
  public IniFile parse(String filepath) throws IOException {
    Path path           = Paths.get(filepath);
    byte[] fileContents = Files.readAllBytes(path);
    String fileSrc      = new String(fileContents);

    this.src    = fileSrc;
    this.cursor = 0;

    ArrayList<String> sections = new ArrayList<>();
    ArrayList<String> keys     = new ArrayList<>();
    ArrayList<String> values   = new ArrayList<>();

    skipWhitespace();

    while (!isAtEOF()) {
      switch (peek()) {
        case '\n': {
          this.line++;
          this.cursor++;
          break;
        }
        case '[': {
          this.cursor++;
          StringBuilder sectionBuilder = new StringBuilder();

          while (!isAtEOF() && peek() != ']') {
            sectionBuilder.append(peek());
            this.cursor++;
          }

          String section = sectionBuilder.toString();
          this.currentSection = section;

          if (!isAtEOF()) { this.cursor++; } // Move past the ']' character
          break;
        }
        case ';':
        case '#': {
          readComment();
          if (!isAtEOF()) { this.cursor++; } // Move cursor out of the comment
        }
        /* WHITESPACE */
        case ' ':
        case '\f':
        case '\r':
        case '\t': {
          skipWhitespace();
          break;
        }
        default: {
          if (Character.isAlphabetic(peek())) {
            readKVPair(sections, keys, values);
          } else {
            this.cursor++; // advance to avoid infinite loop
          }
        }
      }
    }

    for (int idx = 0; idx < sections.size(); idx += 1)
    {
      String section = sections.get(idx);
      String key     = keys.get(idx);
      String value   = values.get(idx);
      System.out.println("[" + idx + "] SECTION: " + section + " | Key-Value: " + key + " = " + value);
    }
    //sections.forEach(System.out::println);
    //keys.forEach(System.out::println);
    //values.forEach(System.out::println);

    // Reset the state of the parser
    this.reset();

    //String[] sectionsArray = sections.toArray(new String[0]);
    //String[] keysArray     = keys.toArray(new String[0]);
    //String[] valuesArray   = values.toArray(new String[0]);

    int size = sections.size();
    ArrayList<Integer> indices = new ArrayList<>(size);
    for (int idx = 0; idx < size; idx += 1) {
      indices.add(idx);
    }

    indices.sort((a, b) -> {
      int sectionCompare = sections.get(a).compareTo(sections.get(b));
      if (sectionCompare != 0) {
        return sectionCompare;
      }

      return keys.get(a).compareTo(keys.get(b));
    });

    String[] sectionsArray = new String[size];
    String[] keysArray     = new String[size];
    String[] valuesArray   = new String[size];

    for (int idx = 0; idx < size; idx += 1) {
      int sortedIndex    = indices.get(idx);

      sectionsArray[idx] = sections.get(sortedIndex);
      keysArray[idx]     = keys.get(sortedIndex);
      valuesArray[idx]   = values.get(sortedIndex);
    }

    IniFile result = new IniFile(sectionsArray, keysArray, valuesArray);
    return result;
  }

  public void skipWhitespace() {
    while (!isAtEOF() && peek()  == ' '  ||
           !isAtEOF() && peek()  == '\f' ||
           !isAtEOF() && peek()  == '\t' ||
           !isAtEOF() && peek()  == '\r'
    ) {
      this.cursor++;
    }
  }

  public void readComment() {
    while (!isAtEOF() && peekAhead() != '\n' && peekAhead() != '\0') {
      this.cursor++;
    }
  }

  public void readKVPair(ArrayList<String> sections, ArrayList<String> keys, ArrayList<String> values) {
    StringBuilder keyBuilder   = new StringBuilder();
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
        skipWhitespace(); // Skip any possible left side whitespace
        while (!isAtEOF() && peek() != '\n') {
          // Skip comments
          if (peek() == ';' || peek() == '#') {
            readComment();
          }
          else {
            valueBuilder.append(peek());
          }
          this.cursor++;
        }
        break;
      }

      if (!isAtEOF()) { this.cursor++; }
    }

    String key   = keyBuilder.toString();
    String value = valueBuilder.toString().trim();

    sections.add(this.currentSection);
    keys.add(key);
    values.add(value);
  }

  public char peekAhead() {
    if (this.cursor + 1 >= this.src.length()) { return '\0'; } // Prevents out-of-bounds on last char
    return this.src.charAt(this.cursor + 1);
  }

  public char peek() {
    if (isAtEOF()) { return '\0'; }
    return this.src.charAt(this.cursor);
  }

  public boolean isAtEOF() {
    return this.cursor >= this.src.length();
  }

  /**
   * Resets cursor, line, src and currentSection for next parse.
   */
  public void reset() {
    this.cursor         = 0;
    this.line           = 1;
    this.src            = null;
    this.currentSection = null;
  }
}
