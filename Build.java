import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.ProcessBuilder;
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

    @Override
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
    String command = args.length > 0 ? args[0] : "build";

    if (command.equals("build"))
    {
      compile();
    }
    else if (command.equals("test"))
    {
      compile();
      runTests();
    }
    else
    {
      println("Unknown command: " + command);
      println("Usage: java Build[.java] [build|test]");
      System.exit(1);
    }
  }

  public static void compile() throws Exception
    {
      new File("classes").mkdirs();

      String[] compilerCommands = new String[2 + SOURCE_FILES.length];
      compilerCommands[0] = "-d";
      compilerCommands[1] = OUTPUT_DIR;

      int sourcePosition = 0;
      int destinationPosition = 2;
      System.arraycopy(SOURCE_FILES, sourcePosition, compilerCommands, destinationPosition, SOURCE_FILES.length);

      boolean compiledSuccessful = tryCompileWithJavaCompilerApi(compilerCommands);
  
      // Fallback for Java 5 using external ProcessBuilder
      if (compiledSuccessful == false)
      {
        // ProcessBuilder requires the actual executable name as the first argument
        List<String> processCommand = new ArrayList<String>();
        processCommand.add("javac");
        for (int idx = 0; idx < compilerCommands.length; idx += 1)
        {
          processCommand.add(compilerCommands[idx]);
        }
  
        ProcessBuilder processBuilder = new ProcessBuilder(processCommand);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
  
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out);
        streamGobbler.start();
  
        int exitCode = process.waitFor();
        streamGobbler.join();
  
        if (exitCode == 0)
        {
          printCompilationInfo();
        }
        else 
        {
          System.err.println("Compilation error with exit code: " + exitCode);
          System.exit(1);
        }
      }
      else 
      {
        printCompilationInfo();
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

      // Locate the run() method with the exact parameter types it expects
      Method runMethod = javaCompiler.getClass().getMethod(
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
      System.err.println("[Warning] Missing javax.tools (Java 5 or earlier java version)");
      return false;
    } catch (Exception exception) {
      System.err.println("Unknown Error during java 6 compatible compilation");
      //exception.printStackTrace();
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
      "src/test/java/org/iniparser/AppTest.java"
    };

    boolean testCompiled = tryCompileWithJavaCompilerApi(compilerCommands);
    if (testCompiled == false)
    {
      List<String> processCommands = new ArrayList<String>();
      processCommands.add("javac");
      processCommands.add("-cp");
      processCommands.add(classpath);
      processCommands.add("-d");
      processCommands.add(OUTPUT_DIR);
      processCommands.add("src/test/java/org/iniparser/AppTest.java");

      ProcessBuilder processBuilder = new ProcessBuilder(processCommands);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();

      StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out);
      streamGobbler.start();
  
      int exitCode = process.waitFor();
      streamGobbler.join();

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

    ProcessBuilder processBuilder = new ProcessBuilder(
        "java",
        "-cp", classpath,
        "org.junit.runner.JUnitCore",
        "org.iniparser.AppTest"
    );

    boolean inheritedIO = false;
    try {
      // Let's see if we have inheritIO method available (requires Java 7+)
      Method inheritIO = ProcessBuilder.class.getMethod("inheritIO");
      inheritIO.invoke(processBuilder);
      inheritedIO = true;
    } catch (NoSuchMethodException noSuchMethodException) {
      //System.out.println("ProcessBuilder.inheritIO method not available (requires Java +7)");
    } catch (Exception exception) {
      System.err.println("Error configuring ProcessBuilder IO");
      exception.printStackTrace();
    }

    Process process = processBuilder.start();
    if (inheritedIO == false)
    {
      StreamGobbler errorGobbler  = new StreamGobbler(process.getErrorStream(), System.err);
      StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out);

      errorGobbler.start();
      outputGobbler.start();
    }

    int exitcode = process.waitFor();
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

  public static String getTestClasspath()
  {
    StringBuilder classpath = new StringBuilder(OUTPUT_DIR);
    File libDir = new File("lib");

    if (libDir.exists() && libDir.isDirectory())
    {
      for (File file : libDir.listFiles())
      {
        if (file.getName().endsWith(".jar"))
        {
          classpath
            .append(CLASSPATH_SEPARATOR)
            .append(file.getPath());
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

    StringBuilder stringBuilderJavaInfo = new StringBuilder();
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
