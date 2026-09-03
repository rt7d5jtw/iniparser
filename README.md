# INI File Parser (Java 17)

### Requirements

* Java 17

---

### How to build?

Java +11
```console
java Build.java
```

Java <11
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

Java +11
```console
java Build test
```

Java <11
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

* **Comments** start with `;` or `#` (inline comments are ignored at the end of a value).
* **Section headers** start with `[` and end with a `]`.
* **Key-Value Pair** contains a key and a value, separated by an `=`.

The parsed values are stored in an **`IniFile`** object, which is returned by the `parse` method. The `IniFile` object encapsulates the configuration data (`HashMap<String, HashMap<String, String>>`) and provides a method (`getValue()`) for retrieval.
