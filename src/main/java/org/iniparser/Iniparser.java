package org.iniparser;

import java.util.Arrays;
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

    int sectionSize = sections.size();
    int[] indexes = new int[sectionSize];
    for (int idx = 0; idx < sectionSize; idx += 1) {
      indexes[idx] = idx;
    }

    bubbleSort(sections, keys, values, indexes);
    // DEBUG PRINT
    for (int idx = 0; idx < sections.size(); idx += 1)
    {
      String section = sections.get(idx);
      String key     = keys.get(idx);
      String value   = values.get(idx);
      System.out.println("[SECTION: " + section + "] " + "[" + idx + "] Key: " + key + " = " + value);
    }
    System.out.println("Indexes: " + Arrays.toString(indexes));

    // Fill the fixed-length arrays
    String[] sectionsArray = new String[sectionSize];
    String[] keysArray     = new String[sectionSize];
    String[] valuesArray   = new String[sectionSize];

    for (int idx = 0; idx < sectionSize; idx += 1) {
      String section = sections.get(idx);
      String key     = keys.get(idx);
      String value   = values.get(idx);

      sectionsArray[idx] = section;
      keysArray[idx]     = key;
      valuesArray[idx]   = value;
    }

    // DEBUG PRINT
    for (int idx = 0; idx < sectionSize; idx += 1)
    {
      String section = sectionsArray[idx];
      String key     = keysArray[idx];
      String value   = valuesArray[idx];
      System.out.println("meowmeow [SECTION: " + section + "] " + "[" + idx + "] Key: " + key + " = " + value);
    }

    // Reset the state of the parser
    this.reset();

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

  public void bubbleSort(
      ArrayList<String> sectionsArray,
      ArrayList<String> keysArray,
      ArrayList<String> valuesArray,
      int[] indices
  ) {
    for (int idx = 0; idx < sectionsArray.size() - 1; idx += 1) {
      boolean swapped = false;

      for (int j = 0; j < sectionsArray.size() - (idx + 1); j += 1) {
        String str1 = sectionsArray.get(j);
        String str2 = sectionsArray.get(j + 1);
        int sectionCompareResult = str2.compareToIgnoreCase(str1);

        if (sectionCompareResult < 0) {
          sectionsArray.set(j + 1, str1);
          sectionsArray.set(j, str2);
          swapped = true;

          // Synchronize keys and values with the index changes
          String key1 = keysArray.get(j);
          String key2 = keysArray.get(j + 1);
          keysArray.set(j, key2);
          keysArray.set(j + 1, key1);

          String value1 = valuesArray.get(j);
          String value2 = valuesArray.get(j + 1);
          valuesArray.set(j, value2);
          valuesArray.set(j + 1, value1);

          // Record the change in array indexes.
          // We need to swap the indexes, but we must preserve the original values.
          int indexpos1 = indices[j];
          int indexpos2 = indices[j + 1];
          indices[j]    = indexpos2;
          indices[j+1]  = indexpos1;
        }
        else if (sectionCompareResult == 0) {
          // Sections are the same, lets compare the keys to see if we can sort them
          String key1 = keysArray.get(j);
          String key2 = keysArray.get(j + 1);
          int keyCompareResult = key2.compareToIgnoreCase(key1);
          if (keyCompareResult < 0) {
            keysArray.set(j, key2);
            keysArray.set(j + 1, key1);
            // We have to also change the indexes of the sections and values to keep the indexes stable.
            // Sync the values
            String value1 = valuesArray.get(j);
            String value2 = valuesArray.get(j + 1);
            valuesArray.set(j, value2);
            valuesArray.set(j + 1, value1);
            // Sections
            sectionsArray.set(j + 1, str1);
            sectionsArray.set(j, str2);

            swapped = true;

            int keyidxpos1 = indices[j];
            int keyidxpos2 = indices[j + 1];
            indices[j]     = keyidxpos2;
            indices[j + 1] = keyidxpos1;
          }
        }
      }

      if (swapped == false) {
        break;
      }
    }
  }
}
