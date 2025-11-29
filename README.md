# INI File Parser (Java 17)

### Requirements

* Java 17

---

### How to build?

* **Maven:** `$ mvn compile`
* **Makefile:** `$ make compile` (or just `$ make` which calls `compile` and `jar`)

---

### How to run tests?

The project uses JUnit 4 for unit testing.

* **With Maven (Recommended):** `$ mvn test` (or `$ mvn verify` for the full lifecycle)
* **With Makefile (Manual Setup Required):**
    1.  Install the JUnit dependency: `make install-test-dependencies`
    2.  Run the tests: `$ make run-tests`

> **Note for Makefile:** The `run-tests` command requires the `junit-4.8.2.jar` file to be present in the `libs/` directory.

---

### About the Parser

This parser implements key-value pair and section parsing inspired by the [EditorConfig File Format Specification](https://spec.editorconfig.org/#file-format).

The parser also allows for inline comments and handles multiline values.

* **Comments** start with `;` or `#` (inline comments are ignored at the end of a value).
* **Section headers** start with `[` and end with a `]`.
* **Key-Value Pair** contains a key and a value, separated by an `=`.

The parsed values are stored in an **`IniFile`** object, which is returned by the `parse` method. The `IniFile` object encapsulates the configuration data (`HashMap<String, HashMap<String, String>>`) and provides a method (`getValue()`) for retrieval.
