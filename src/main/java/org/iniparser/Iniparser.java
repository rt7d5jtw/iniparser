package org.iniparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

public class Iniparser {
  private String src = null;
  private int cursor = 0;
  private int line   = 0;
  private String currentSection = "global";

  /**
   * Parses the INI file contents read from the given filepath.
   * @param filepath The path to the INI file.
   * @return An Optional containing the fully parsed IniFile object.
   * @throws IOException If the file cannot be read.
   */
  public IniFile parse(String filepath) throws IOException {
    String fileSrc = readTextFile(filepath);

    this.src    = fileSrc;
    this.cursor = 0;

    ArrayList<String> sections = new ArrayList<String>();
    ArrayList<String> keys     = new ArrayList<String>();
    ArrayList<String> values   = new ArrayList<String>();

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
          skipComment();
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
          if (Character.isLetter(peek())) {
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

    // Sort the arrays
    int startIndex = 0;
    int endIndex   = sections.size() - 1;
    quicksort(startIndex, endIndex, sections, keys, indexes);

    // Fill the fixed-length arrays
    String[] sectionsArray = new String[sectionSize];
    String[] keysArray     = new String[sectionSize];
    String[] valuesArray   = new String[sectionSize];

    for (int idx = 0; idx < sectionSize; idx += 1) {
      int sortedIndex = indexes[idx];

      String section = sections.get(sortedIndex);
      String key     = keys.get(sortedIndex);
      String value   = values.get(sortedIndex);

      sectionsArray[idx] = section;
      keysArray[idx]     = key;
      valuesArray[idx]   = value;
    }

   // Reset the state of the parser
    this.reset();

    IniFile result = new IniFile(sectionsArray, keysArray, valuesArray);
    return result;
  }

  public String readTextFile(String filepath) throws IOException {
    File file = new File(filepath);
    StringBuilder stringBuilder = new StringBuilder();
    String fileSource     = null;
    FileReader fileReader = null;
    BufferedReader reader = null;
    String lineSeparator  = System.getProperty("line.separator");
    String line;

    try {
      fileReader = new FileReader(file);
      reader     = new BufferedReader(fileReader);

      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append(lineSeparator);
      }

      fileSource = stringBuilder.toString();

    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ioexception) {
          System.err.println("Failed to close the BufferedReader!");
        }
      }
    }

    return fileSource;
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

  public void skipComment() {
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
            skipComment();
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
    this.currentSection = "global";
  }

  public int medianOfThree(ArrayList<String> array, int[] indexes, int index1, int index2, int index3)
  {
    String value1 = array.get(indexes[index1]);
    String value2 = array.get(indexes[index2]);
    String value3 = array.get(indexes[index3]);

    if (value1.compareTo(value2) >= 0 && value1.compareTo(value3) <= 0 ||
        value1.compareTo(value2) <= 0 && value1.compareTo(value3) >= 0)
    {
      return index1;
    }
    if (value2.compareTo(value1) >= 0 && value2.compareTo(value3) <= 0 ||
        value2.compareTo(value1) <= 0 && value2.compareTo(value3) >= 0)
    {
      return index2;
    }

    return index3;
  }

  public void quicksort(
      int startIndex,
      int endIndex,
      ArrayList<String> sectionArray,
      ArrayList<String> keyArray,
      int[] indexes
      )
  {
    if (startIndex >= endIndex)
    {
      return;
    }

    int midpoint = ((startIndex + endIndex) / 2);
    int pivotIndex = medianOfThree(sectionArray, indexes, startIndex, midpoint, endIndex);
    String pivotValue = sectionArray.get(indexes[pivotIndex]);

    int temporaryIndex = indexes[endIndex];
    indexes[endIndex] = indexes[pivotIndex];
    indexes[pivotIndex] = temporaryIndex;

    // prefetch pivot strings
    String pivotSection = sectionArray.get(indexes[endIndex]);
    String pivotKey     = keyArray.get(indexes[endIndex]);

    int boundaryIndex = startIndex;
    for (int scannerIndex = startIndex; scannerIndex < endIndex; scannerIndex += 1)
    {
      String currentSection = sectionArray.get(indexes[scannerIndex]);
      int sectionComparison = currentSection.compareTo(pivotSection);

      int keyComparison      = 0;
      boolean keyPrecesPivot = false;

      // Same section, calculate key comparison
      if (sectionComparison == 0)
      {
        String currentKey = keyArray.get(indexes[scannerIndex]);
        keyComparison     = currentKey.compareTo(pivotKey);
        keyPrecesPivot    = (keyComparison < 0);
      }

      if (sectionComparison < 0 || keyPrecesPivot)
      {
        int boundary = indexes[boundaryIndex];
        indexes[boundaryIndex] = indexes[scannerIndex];
        indexes[scannerIndex]  = boundary;
        boundaryIndex += 1;
      }
    }

    int tmpPivotIndex = indexes[endIndex];
    indexes[endIndex] = indexes[boundaryIndex];
    indexes[boundaryIndex] = tmpPivotIndex;

    quicksort(startIndex, boundaryIndex - 1, sectionArray, keyArray, indexes);
    quicksort(boundaryIndex + 1, endIndex, sectionArray, keyArray, indexes);
  }
}
