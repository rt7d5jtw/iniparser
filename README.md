# INI File Parser

### Requirements

- Java 5+
  - Tested on Sun/Oracle JDK 1.5 (Version: 1.5.0_10, Specification: 1.5, VM: Java HotSpot(TM) 64-Bit Server VM 1.5.0_10-b03)
  - Tested on Zulu OpenJDK 1.6.0-119
  - Tested on modern JDKs (Java 11+)

![Java 5 Compatibility Build Output](terminal.png)

---

### How to build?

Java 11+
```console
java Build.java
```

Java 5 – 10
```console
javac Build.java && java Build
```

Maven:
```console
mvn compile
```

---

### How to run tests?

The project uses JUnit 4.8.2 for unit testing. The library is vendored in the `lib/` directory.

Java 11+
```console
java Build test
```

Java 5 – 10
```console
javac Build.java && java Build test
```

Maven:
```console
mvn test
```

---

### About the Parser

This parser implements key-value pair and section parsing inspired by the [EditorConfig File Format Specification](https://spec.editorconfig.org/#file-format).

The parser also allows for inline comments and handles multiline values.

- **Comments** start with `;` or `#` (inline comments are ignored at the end of a value).
- **Section headers** start with `[` and end with a `]`.
- **Key-Value Pair** contains a key and a value, separated by an `=`.

The parsed values are stored in an **`IniFile`** object, which is returned by the `parse` method. The `IniFile` object encapsulates the configuration data and provides a method (`getValue(String section, String key)`) for retrieval.
