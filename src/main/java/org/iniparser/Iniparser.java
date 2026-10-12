package org.iniparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Iniparser {
  private String src = null;
  private int cursor = 0;
  private int line   = 1;
  private String currentSection = "global";

  /**
   * Parses the INI file contents read from the given filepath.
   * @param filepath The path to the INI file.
   * @return The fully parsed IniFile object.
   * @throws IOException If the file cannot be read or contains a syntax error.
   */
  public IniFile parse(String filepath) throws IOException {
    this.src = readTextFile(filepath);
    this.cursor = 0;
    this.line = 1;

    ArrayList<String> sectionList = new ArrayList<String>();
    ArrayList<String> keyList     = new ArrayList<String>();
    ArrayList<String> valueList   = new ArrayList<String>();

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

          while (!isAtEOF() && peek() != ']' && peek() != '\n') {
            sectionBuilder.append(peek());
            this.cursor++;
          }

          if (peek() != ']') {
            throw new IOException("Syntax error in '" + filepath + "' at line " + this.line + ": Missing closing ']' for section header.");
          }

          this.currentSection = sectionBuilder.toString().trim();
          this.cursor++; // Move past the ']' character
          break;
        }
        case ';':
        case '#': {
          skipComment();
          // We intentionally do NOT advance the cursor past the '\n' here,
          // so the main loop catches it and increments this.line correctly.
          break;
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
          if (Character.isLetterOrDigit(peek()) || peek() == '.') {
            readKVPair(sectionList, keyList, valueList);
          } else {
            throw new IOException("Syntax error in '" + filepath + "' at line " + this.line + ": Unexpected character '" + peek() + "'.");
          }
        }
      }
    }

    int entryCount = sectionList.size();
    int[] sortIndexes = new int[entryCount];
    for (int idx = 0; idx < entryCount; idx += 1) {
      sortIndexes[idx] = idx;
    }

    // Sort the arrays
    if (entryCount > 0) {
      quicksort(0, entryCount - 1, sectionList, keyList, sortIndexes);
    }

    // Fill the fixed-length arrays
    String[] sectionsArray = new String[entryCount];
    String[] keysArray     = new String[entryCount];
    String[] valuesArray   = new String[entryCount];

    for (int idx = 0; idx < entryCount; idx += 1) {
      int sortedIndex = sortIndexes[idx];

      sectionsArray[idx] = sectionList.get(sortedIndex);
      keysArray[idx]     = keyList.get(sortedIndex);
      valuesArray[idx]   = valueList.get(sortedIndex);
    }

    // Reset the state of the parser
    this.reset();

    return new IniFile(sectionsArray, keysArray, valuesArray);
  }

  public String readTextFile(String filepath) throws IOException {
    File file = new File(filepath);
    if (!file.exists() || !file.isFile()) {
      throw new IOException("Failed to read INI file: '" + filepath + "' does not exist or is not a valid file.");
    }

    StringBuilder stringBuilder = new StringBuilder();
    BufferedReader reader = null;
    String lineSeparator  = System.getProperty("line.separator");
    String currentLine;

    try {
      reader = new BufferedReader(new FileReader(file));

      while ((currentLine = reader.readLine()) != null) {
        stringBuilder.append(currentLine).append(lineSeparator);
      }
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException ioexception) {
          System.err.println("Warning: Failed to safely close the file reader for '" + filepath + "'.");
        }
      }
    }

    return stringBuilder.toString();
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
    while (!isAtEOF() && peek() != '\n') {
      this.cursor++;
    }
  }

  public void readKVPair(ArrayList<String> sectionList, ArrayList<String> keyList, ArrayList<String> valueList) throws IOException {
    StringBuilder keyBuilder   = new StringBuilder();
    StringBuilder valueBuilder = new StringBuilder();
    boolean foundEquals = false;

    while (!isAtEOF() && peek() != '\n') {
      // Read the key
      while (!isAtEOF() && peek() != '=' && peek() != '\n') {
        if (!Character.isWhitespace(peek())) {
          keyBuilder.append(peek());
        }
        this.cursor++;
      }

      // Read the value
      if (!isAtEOF() && peek() == '=') {
        foundEquals = true;
        this.cursor++; // Move cursor from `=`
        skipWhitespace(); // Skip any possible left side whitespace

        while (!isAtEOF() && peek() != '\n') {
          // backlash means multi-line strings
          if (peek() == '\\') {
            this.cursor += 2; // move past '\' and newline
            this.line++; // account for the newline character bypassed
            skipWhitespace(); // skip whitespace until we find a character on the new line
            continue;
          }
          // Skip comments
          if (peek() == ';' || peek() == '#') {
            skipComment();
            break;
          } else {
            valueBuilder.append(peek());
          }
          this.cursor++;
        }
        break;
      }

      if (!isAtEOF() && peek() != '\n') { this.cursor++; }
    }

    if (!foundEquals) {
      throw new IOException("Syntax error at line " + this.line + ": Key-value pair '" + keyBuilder.toString() + "' is missing an '=' character.");
    }

    sectionList.add(this.currentSection);
    keyList.add(keyBuilder.toString());
    valueList.add(valueBuilder.toString().trim());
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
      ArrayList<String> sectionList,
      ArrayList<String> keyList,
      int[] indexes
      )
  {
    if (startIndex >= endIndex)
    {
      return;
    }

    int midpoint = ((startIndex + endIndex) / 2);
    int pivotIndex = medianOfThree(sectionList, indexes, startIndex, midpoint, endIndex);

    int tempPivotSwap = indexes[endIndex];
    indexes[endIndex] = indexes[pivotIndex];
    indexes[pivotIndex] = tempPivotSwap;

    // prefetch pivot strings
    String pivotSection = sectionList.get(indexes[endIndex]);
    String pivotKey     = keyList.get(indexes[endIndex]);

    int boundaryIndex = startIndex;
    for (int scannerIndex = startIndex; scannerIndex < endIndex; scannerIndex += 1)
    {
      String currentSection = sectionList.get(indexes[scannerIndex]);
      int sectionComparison = currentSection.compareTo(pivotSection);

      int keyComparison      = 0;
      boolean keyPrecedesPivot = false;

      // Same section, calculate key comparison
      if (sectionComparison == 0)
      {
        String currentKey = keyList.get(indexes[scannerIndex]);
        keyComparison     = currentKey.compareTo(pivotKey);
        keyPrecedesPivot  = (keyComparison < 0);
      }

      if (sectionComparison < 0 || keyPrecedesPivot)
      {
        int boundary = indexes[boundaryIndex];
        indexes[boundaryIndex] = indexes[scannerIndex];
        indexes[scannerIndex]  = boundary;
        boundaryIndex += 1;
      }
    }

    int tempBoundarySwap = indexes[endIndex];
    indexes[endIndex] = indexes[boundaryIndex];
    indexes[boundaryIndex] = tempBoundarySwap;

    quicksort(startIndex, boundaryIndex - 1, sectionList, keyList, indexes);
    quicksort(boundaryIndex + 1, endIndex, sectionList, keyList, indexes);
  }
}
