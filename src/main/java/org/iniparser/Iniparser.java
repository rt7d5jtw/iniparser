package org.iniparser;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PushbackInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Iniparser {
  private String src = null;
  private int cursor = 0;
  private int line   = 1;
  private String currentSection = "global";

  private static final int[][] BOMS = {
          { 0xEF, 0xBB, 0xBF }, // UTF-8
          { 0xFE, 0xFF },       // UTF-16BE
          { 0xFF, 0xFE }        // UTF-16LE
  };

  private static final String[] ENCODING_NAMES = {
          "UTF-8",
          "UTF-16BE",
          "UTF-16LE"
  };

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

    ArrayList sectionList = new ArrayList();
    ArrayList keyList     = new ArrayList();
    ArrayList valueList   = new ArrayList();

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
          StringBuffer sectionBuilder = new StringBuffer();

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

      sectionsArray[idx] = (String) sectionList.get(sortedIndex);
      keysArray[idx]     = (String) keyList.get(sortedIndex);
      valuesArray[idx]   = (String) valueList.get(sortedIndex);
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

    StringBuffer stringBuilder = new StringBuffer();
    InputStream fileStream     = null;
    String currentLine;

    try {
      fileStream = new FileInputStream(file);
      // pushback buffer must be at least as large as the longest BOM (3 bytes)
      PushbackInputStream pushbackStream = new PushbackInputStream((fileStream), 3);
      String encodingName = detectAndSkipBOM(pushbackStream);
      Reader decoder;

      if (encodingName != null)
      {
        decoder = new InputStreamReader(pushbackStream, encodingName);
      }
      else
      {
        // No BOM
        decoder = new InputStreamReader(pushbackStream);
      }

      BufferedReader reader = new BufferedReader(decoder);
      while ((currentLine = reader.readLine()) != null)
      {
        stringBuilder.append(currentLine).append('\n');
      }
    } finally {
      if (fileStream != null) {
        try {
          fileStream.close();
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

  public void readKVPair(ArrayList sectionList, ArrayList keyList, ArrayList valueList) throws IOException {
    StringBuffer keyBuilder   = new StringBuffer();
    StringBuffer valueBuilder = new StringBuffer();
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
          if (peek() == '\\' && peekAhead() == '\n') {
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

  public char peekAhead()
  {
    if (isAtEOF())
    {
      return '\0';
    }

    if (cursor >= this.src.length())
    {
      return '\0';
    }

    return this.src.charAt(this.cursor + 1);
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

  public int medianOfThree(ArrayList array, int[] indexes, int index1, int index2, int index3)
  {
    String value1 = (String) array.get(indexes[index1]);
    String value2 = (String) array.get(indexes[index2]);
    String value3 = (String) array.get(indexes[index3]);

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
      ArrayList sectionList,
      ArrayList keyList,
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
    String pivotSection = (String) sectionList.get(indexes[endIndex]);
    String pivotKey     = (String) keyList.get(indexes[endIndex]);

    int boundaryIndex = startIndex;
    for (int scannerIndex = startIndex; scannerIndex < endIndex; scannerIndex += 1)
    {
      String currentSection = (String) sectionList.get(indexes[scannerIndex]);
      int sectionComparison = currentSection.compareTo(pivotSection);

      int keyComparison      = 0;
      boolean keyPrecedesPivot = false;

      // Same section, calculate key comparison
      if (sectionComparison == 0)
      {
        String currentKey = (String) keyList.get(indexes[scannerIndex]);
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

  public static String detectAndSkipBOM(PushbackInputStream inputStream) throws IOException
  {
    int maxBOMLength = 3;
    byte[] headerBuffer = new byte[maxBOMLength];

    int bytesRead = 0;
    while (bytesRead < headerBuffer.length)
    {
      int bytesInThisRead = inputStream.read(headerBuffer, bytesRead, headerBuffer.length - bytesRead);
      if (bytesInThisRead < 0)
      {
        break;
      }

      bytesRead += bytesInThisRead;
    }

    // find the first BOM signature that matches the start of the buffer
    int matchedBOMIndex= -1;
    for (int bomIndex = 0; bomIndex < BOMS.length && matchedBOMIndex < 0; bomIndex += 1)
    {
      if (matches(headerBuffer, bytesRead, BOMS[bomIndex]))
      {
        matchedBOMIndex = bomIndex;
      }
    }

    boolean bomFound = (matchedBOMIndex >= 0);
    int bomLength = bomFound ? BOMS[matchedBOMIndex].length : 0;
    int bytesToPushBack = bytesRead - bomLength;
    if (bytesToPushBack > 0)
    {
      inputStream.unread(headerBuffer, bomLength, bytesToPushBack);
    }

    return bomFound ? ENCODING_NAMES[matchedBOMIndex] : null;
  }

  private static boolean matches(byte[] buffer, int bytesRead, int[] BOMSignature)
  {
    if (bytesRead < BOMSignature.length)
    {
      return false;
    }

    for (int idx = 0; idx < BOMSignature.length; idx += 1)
    {
      int actualByte   = buffer[idx] & 0xFF;
      int expectedByte = BOMSignature[idx];

      if (actualByte != expectedByte)
      {
        return false;
      }
    }

    return true;
  }
}
