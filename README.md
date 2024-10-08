### How to build?

`$ mvn compile`

### How to run tests?

`$ mvn verify`

### About the parser

Since INI is an informal format, I decided to go with the EditorConfig-like specification [File Format](https://spec.editorconfig.org/#file-format).

Note: This parser also allows for inline comments!

* Comments start with `;` or `#`
* Section headers start with `[` and ends with a `]`
* Key-Value Pair contains a key and a value, separated by an `=`.
  - Key: the part before the first `=` (trimmed of whitespace)
  - Value: the part after the first `=` (trimmed of whitespace)


The values are stored in IniMap class that the `parse` method returns, that is just a HashMap that stores another HashMap by sections.

### Requirements

* Java 17

