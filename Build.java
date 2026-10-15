import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;

public class Build
{

  /* A Class to read input stream into an output stream from another process for Java 6 Compatibility */
  // StreamGobbler {{{
  private static class StreamGobbler extends Thread
  {
    private final InputStream inputStream;
    private final PrintStream outputStream;

    StreamGobbler(InputStream inputStream, PrintStream outputStream)
    {
      this.inputStream  = inputStream;
      this.outputStream = outputStream;
    }

    public void run()
    {
      String line;
      BufferedReader reader = null;

      try {
        reader = new BufferedReader(new InputStreamReader(inputStream));
        while ((line = reader.readLine()) != null) {
          outputStream.println(line); // forward the input stream
        }
      } catch (IOException ioexception) {
        System.err.println("Error forwarding output stream!");
        ioexception.printStackTrace();
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException ioexception) {
            System.err.println("Error: Could not close the reader");
          }
        }
      }
    }
  }
  // }}}

  // Constants {{{

  public static final String TEST_FILE = "src/test/java/org/iniparser/AppTest.java";
  public static final String CLASSPATH_SEPARATOR = File.pathSeparator;
  public static final String LIBS = "lib/*";
  public static final String OUTPUT_DIR = "classes";
  public static final String[] SOURCE_FILES = {
    "src/main/java/org/iniparser/Iniparser.java",
    "src/main/java/org/iniparser/IniFile.java"
  };
  // }}}

  public static void main(String[] args) throws Exception
  {
    String command  = "build";
    boolean verbose = false;

    // Parse command line arguments
    for (int i = 0; i < args.length; i++)
    {
      if (args[i].equals("-v") || args[i].equals("--verbose"))
      {
        verbose = true;
      }
      else
      {
        command = args[i];
      }
    }

    if (command.equals("build"))
    {
      compile(verbose);
    }
    else if (command.equals("test"))
    {
      compile(verbose);
      runTests();
    }
    else
    {
      println("Unknown command: " + command);
      println("Usage: java Build[.java] [-v|--verbose] [build|test]");
      System.exit(1);
    }
  }

  public static void compile(boolean verbose) throws Exception
    {
      new File("classes").mkdirs();

      String[] compilerCommands = new String[2 + SOURCE_FILES.length];
      compilerCommands[0] = "-d";
      compilerCommands[1] = OUTPUT_DIR;

      int sourcePosition = 0;
      int destinationPosition = 2;
      System.arraycopy(SOURCE_FILES, sourcePosition, compilerCommands, destinationPosition, SOURCE_FILES.length);

      boolean compiledSuccessful = tryCompileWithJavaCompilerApi(compilerCommands);

      // Fallback for Java 5 using external process execution
      if (compiledSuccessful == false)
      {
        ArrayList processCommand = new ArrayList();
        processCommand.add("javac");
        for (int idx = 0; idx < compilerCommands.length; idx += 1)
        {
          processCommand.add(compilerCommands[idx]);
        }

        String[] cmdArray = new String[processCommand.size()];
        for (int idx = 0; idx < processCommand.size(); idx += 1)
        {
          cmdArray[idx] = (String) processCommand.get(idx);
        }

        Process process = Runtime.getRuntime().exec(cmdArray);

        StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);

        errorGobbler.start();
        outputGobbler.start();

        int exitCode = process.waitFor();
        errorGobbler.join();
        outputGobbler.join();

        if (exitCode == 0)
        {
          if (verbose) { printCompilationInfo(); }
        }
        else
        {
          System.err.println("Compilation error with exit code: " + exitCode);
          System.exit(1);
        }
      }
      else
      {
        if (verbose) { printCompilationInfo(); }
      }
    }

  public static boolean tryCompileWithJavaCompilerApi(String[] compilerCommands)
  {
    try {
      Class toolProvider = Class.forName("javax.tools.ToolProvider");

      // Fetch and invoke the static getSystemJavaCompiler method
      Method getCompilerMethod = toolProvider.getMethod("getSystemJavaCompiler", new Class[0]);
      Object javaCompiler = getCompilerMethod.invoke(null, new Object[0]);

      if (javaCompiler == null)
      {
        return false;
      }

      // Fetch the public interface instead of the internal implementation class
      Class toolInterface = Class.forName("javax.tools.Tool");

      // Locate the run() method on the interface
      Method runMethod = toolInterface.getMethod(
          "run",
          new Class[] {
            java.io.InputStream.class,
            java.io.OutputStream.class,
            java.io.OutputStream.class,
            String[].class
          }
      );

      // Package arguments for the run() method
      Object[] invokeArgs = new Object[] {
        null,
        System.out,
        System.err,
        compilerCommands
      };

      // Execute the compiler and check for a successful exit code (0)
      Integer exitCode = (Integer) runMethod.invoke(javaCompiler, invokeArgs);
      return exitCode.intValue() == 0;

    } catch (ClassNotFoundException classNotFoundException) {
      // expected on Java 5 or earlier
      return false;
    } catch (Exception exception) {
      String exceptionMessage = exception.getMessage();
      System.err.println("Unknown Error during java 6 compatible compilation: " + exceptionMessage);
      exception.printStackTrace();
      return false;
    }
  }

  // tests {{{
  public static void runTests() throws Exception
  {
    String classpath = getTestClasspath();
    String[] compilerCommands = new String[] {
      "-cp", classpath,
      "-d", OUTPUT_DIR,
      TEST_FILE
    };

    boolean testCompiled = tryCompileWithJavaCompilerApi(compilerCommands);
    if (testCompiled == false)
    {
      ArrayList processCommands = new ArrayList();
      processCommands.add("javac");
      processCommands.add("-cp");
      processCommands.add(classpath);
      processCommands.add("-d");
      processCommands.add(OUTPUT_DIR);
      processCommands.add(TEST_FILE);

      String[] cmdArray = new String[processCommands.size()];
      for (int idx = 0; idx < processCommands.size(); idx += 1)
      {
        cmdArray[idx] = (String) processCommands.get(idx);
      }

      Process process = Runtime.getRuntime().exec(cmdArray);

      StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
      StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);

      errorGobbler.start();
      outputGobbler.start();

      int exitCode = process.waitFor();
      errorGobbler.join();
      outputGobbler.join();

      if (exitCode != 0)
      {
        System.err.println("Test compilation error");
        System.exit(1);
      }
      else if (exitCode == 0)
      {
        println("Test files successfully compiled");
      }
    }
    else if (testCompiled == true)
    {
      println("Test files successfully compiled");
    }

    // Select the appropriate test runner based on the Java version
    String testRunnerClass = isLegacyJava() ? "junit.textui.TestRunner" : "org.junit.runner.JUnitCore";

    String[] testRunCommands = new String[] {
      "java",
      "-cp", classpath,
      testRunnerClass,
      "org.iniparser.AppTest"
    };

    Process process = Runtime.getRuntime().exec(testRunCommands);

    StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);

    errorGobbler.start();
    outputGobbler.start();

    int exitcode = process.waitFor();
    errorGobbler.join();
    outputGobbler.join();

    if (exitcode != 0)
    {
      System.err.println("Tests failed!");
      System.exit(exitcode);
    }
    else
    {
      println("All tests passed!");
    }
  };

  public static boolean isLegacyJava()
  {
    String specVersion = System.getProperty("java.specification.version");
    return specVersion != null && (specVersion.startsWith("1.3") || specVersion.startsWith("1.4"));
  }

  public static String getTestClasspath()
  {
    StringBuffer classpath = new StringBuffer(OUTPUT_DIR);
    File libraryDirectory = new File("lib");

    if (libraryDirectory.exists() && libraryDirectory.isDirectory())
    {
      File[] files = libraryDirectory.listFiles();
      if (files != null)
      {
        for (int idx = 0; idx < files.length; idx += 1)
        {
          File file = (File) files[idx];
          if (file.getName().endsWith(".jar"))
          {
            String fileName = file.getName();

            // Prevent loading JUnit 4 on legacy Java, and vice versa
            if (isLegacyJava() && fileName.indexOf("junit-4") != -1)
            {
              continue;
            }
            if (!isLegacyJava() && fileName.indexOf("junit-3") != -1)
            {
              continue;
            }

            classpath
              .append(CLASSPATH_SEPARATOR)
              .append(file.getPath());
          }
        }
      }
    }

    return classpath.toString();
  }
  // }}}

  public static void println(String str)
  {
    System.out.println(str);
  }

  // verbose compilation info
  public static void printCompilationInfo()
  {
    String javaVersion   = System.getProperty("java.version");
    String javaSpec      = System.getProperty("java.specification.version");
    String javaVMname    = System.getProperty("java.vm.name");
    String javaVMVersion = System.getProperty("java.vm.version");

    StringBuffer stringBuilderJavaInfo = new StringBuffer();
    stringBuilderJavaInfo.append("[COMPILATION] Compilation successful with\n");
    stringBuilderJavaInfo.append("Java version: ");
    stringBuilderJavaInfo.append(javaVersion);
    stringBuilderJavaInfo.append("\nJava specification: ");
    stringBuilderJavaInfo.append(javaSpec);
    stringBuilderJavaInfo.append("\nJava VM name: ");
    stringBuilderJavaInfo.append(javaVMname);
    stringBuilderJavaInfo.append("\nJava VM Version: ");
    stringBuilderJavaInfo.append(javaVMVersion);
    stringBuilderJavaInfo.append("\n");

    String compilationInfo = stringBuilderJavaInfo.toString();
    println(compilationInfo);
  }
}
