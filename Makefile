# NOTE: curl is used to download the dependencies !

# copy:   https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/copy
# xcopy:  https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/xcopy
# cp:     https://man7.org/linux/man-pages/man1/cp.1.html

ifeq ($(OS),Windows_NT)
    COPY_CMD          := xcopy
    COPY_FLAGS        := /e/q
else
    COPY_CMD          := cp
    COPY_FLAGS        := --verbose --recursive
endif

MAIN_CLASS            := iniparser/Iniparser
SOURCE_DIR            := src
OUTPUT_DIR            := classes
LIB_DIR               := libs
BUILD_DIR             := build
REPO_URI              := repo1.maven.org/maven2

TEST_MAIN_CLASS       := $(SOURCE_DIR)/test/java/org/iniparser/AppTest.java

SOURCE_PATH           := src/main/java/org/iniparser
RESOURCE_PATH         := src/main/resources

# MANIFEST.MF Definitions
MANIFEST_VERSION      := 1.0
MANIFEST_MAIN_CLASS   := org.iniparser.Iniparser
MANIFEST_CREATED_BY   := 17.0.3

# source: https://stackoverflow.com/questions/10571658/gnu-make-convert-spaces-to-colons
# literal space
SPACE                 := $(subst ,, )

# Finds the source files with .java extension under src/main/java/io/what/jhttpd
SOURCES               := $(wildcard $(SOURCE_PATH)/*.java)

# Finds jar's under libs/ directory and formats a colon to end of each jar.
# On Windows, use a semicolon (;) instead of a colon as a separator.
LIBS                  := "$(subst $(SPACE),:,$(wildcard libs/*.jar)):"

# Finds the resources files under src/main/resources/
RESOURCES             := $(wildcard $(RESOURCE_PATH)/*)

# Java tools
JAVA                  := java
JAVAC                 := javac
JFLAGS                := -sourcepath $(SOURCE_DIR) -d $(OUTPUT_DIR) -source 17 -Werror -Xlint:all -g

MANIFEST              := src/main/manifest/MANIFEST.MF
JAR_FILE              := iniparser-1.0.jar
JAR                   := jar
JARFLAGS              := cvfm build/$(JAR_FILE) $(MANIFEST)

JUNIT_VERSION         := 4.8.2
JUNIT_JAR             := junit/junit/$(JUNIT_VERSION)/junit-$(JUNIT_VERSION).jar
JUNIT_MAIN            := org.junit.runner.JUnitCore

all: compile jar

run:
	$(JAVA) -jar build/$(JAR_FILE)

run-verbose:
	$(JAVA) -showversion -verbose:gc -cp ./classes $(MAIN_CLASS)

install-test-dependencies:
	@echo "Installing test dependencies ..."
	curl -v -fL https://$(REPO_URI)/$(JUNIT_JAR) --output $(LIB_DIR)/junit-$(JUNIT_VERSION).jar
	echo "NOTE: CHANGES TO CLASS PATH HAVE TO BE WRITTEN IN $(MANIFEST) !"
	@echo "To update the class path, run 'update-classpath' recipe"

build: compile

# Compiles and runs tests
#
# NOTE: You need jar of the library itself in libs too, libs/iniparser.jar
#				this currently does not work because I cant get the main class to run
#
#				Run `compile` and `jar` before running this so that the iniparser.jar is included!
#				If junit-4.8.2 is not installed in libs/ directory, run `install-test-dependencies`
run-tests: $(SOURCES)
	$(JAVAC) $(JFLAGS) -cp $(LIBS) $(TEST_MAIN_CLASS)
	java -cp $(LIBS) $(JUNIT_MAIN) org.iniparser.AppTest

compile: $(SOURCES)
	@mkdir -vp $(OUTPUT_DIR)
	$(JAVAC) $(JFLAGS) $(SOURCES)
	@echo "Copying resources over from $(RESOURCE_PATH) to $(OUTPUT_DIR)/ ..."
	$(COPY_CMD) $(COPY_FLAGS) $(RESOURCES) $(OUTPUT_DIR)

jar: $(OUTPUT_DIR)
	@mkdir -vp build/
	$(JAR) $(JARFLAGS) -C $(OUTPUT_DIR) .
	@echo "Copying iniparser.jar over to libs/ so it can be used for tests"
	$(COPY_CMD) $(BUILD_DIR)/$(JAR_FILE) libs

inspect: $(OUTPUT_DIR)
	@echo Sources: $(SOURCES)
	@echo Classes: $(wildcard classes/iniparser/*.class)
	@echo Resources: $(RESOURCES)

# Updates the class path reference for the manifest, by creationg a new MANIFEST.MF
# to make the jar easily executable (links to libraries by the Class-Path reference)
update-classpath:
	@echo "Manifest-Version: $(MANIFEST_VERSION)\nMain-Class: $(MANIFEST_MAIN_CLASS)\nClass-Path:\nCreated-By: $(MANIFEST_CREATED_BY)" > $(MANIFEST)
	@echo "MANIFEST.MF Created at $(MANIFEST)"

format: $(SOURCES)
	java -jar google-java-format-1.15.0-all-deps.jar -i $(SOURCES)

sources:
	@echo SOURCES: $(SOURCES)
	@echo RESOURCES: $(RESOURCES)

# Viewing the Contents of a JAR FILE: https://docs.oracle.com/javase/tutorial/deployment/jar/view.html
view-jar:
	jar vtf build/$(JAR_FILE)

clean: $(CLASSES)
	@rm -rf classes
	@rm -rf build

classpath:
	@echo "export CLASSPATH='$(CLASSPATH)'"

.PHONY: all run inspect classpath update-classpath clean format jar compile run-verbose view-jar install-test-dependencies
