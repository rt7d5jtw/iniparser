package org.iniparser;

public class IniFile {
  private final String[] sections;
  private final String[] keys;
  private final String[] values;

  public IniFile(String[] sections, String[] keys, String[] values) {
    this.sections = sections;
    this.keys     = keys;
    this.values   = values;
  }

  public String getValue(String section, String key) {
    int index = binarySearch(section, key);
    if (index == -1) {
      return null;
    }

    return values[index];
  }

  public int binarySearch(String section, String key) {
    int startIndex = 0;
    int endIndex   = sections.length - 1;

    while (startIndex <= endIndex) {
      int midIndex = (startIndex + endIndex) / 2;

      String midSection   = sections[midIndex];
      int sectionCompare  = section.compareTo(midSection);

      if (sectionCompare > 0) {
        startIndex = midIndex + 1;
      }
      else if (sectionCompare < 0) {
        endIndex = midIndex - 1;
      }
      else if (sectionCompare == 0) {
        String midKey  = keys[midIndex];
        int keyCompare = key.compareTo(midKey);

        if (keyCompare > 0) {
          startIndex = midIndex + 1;
        }
        else if (keyCompare < 0) {
          endIndex = midIndex - 1;
        }
        else if (keyCompare == 0) {
          return midIndex;
        }
      }
    }

    return -1;
  }
}
