### How to build?

Maven: `$ mvn compile`

Makefile: `$ make`

### How to run tests?

Maven: `$ mvn verify`

1. Install junit to libs/ directory first, if not installed already: `make install-test-dependencies`
2. Compile the program and build to jar `make compile jar`
3. Run tests: `$ make run-tests`

If you already have junit 4.8.2 installed and jar built for the program, you can skip to step 3 by just running the `run-tests` recipe.

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

