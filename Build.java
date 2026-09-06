import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;

public class Build
{

  /* A Class to read input stream into an output stream from another process for Java 6 Compatibility */
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

  public static final String CLASSPATH_SEPARATOR = File.pathSeparator;
  public static final String LIBS = "lib/*";
  public static final String OUTPUT_DIR = "classes";
  public static final String[] SOURCE_FILES = {
    "src/main/java/org/iniparser/Iniparser.java",
    "src/main/java/org/iniparser/IniFile.java"
  };

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

  public static void compile()
  {
    new File("classes").mkdirs();

    JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    if (javaCompiler == null)
    {
      System.err.println("Error: Java Compiler not found!");
      System.exit(1);
    }

    println("Compiling sources...");
    int exitcode = javaCompiler.run(
        null,
        System.out,
        System.err,
        "-d", "classes",
        SOURCE_FILES[0],
        SOURCE_FILES[1]
    );

    if (exitcode == 0)
    {
      println("Compilation successful");
    }
    else
    {
      System.err.println("Compilation error with exit code: " + exitcode);
      System.exit(1);
    }
  }

  public static void runTests() throws Exception
  {
    JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    if (javaCompiler == null) {
      System.err.println("Error: Java Compiler not found!");
      System.exit(1);
    }

    String classpath = getTestClasspath();
    int compilecode = javaCompiler.run(
        null,
        System.out,
        System.err,
        "-cp", classpath,
        "-d", OUTPUT_DIR,
        "src/test/java/org/iniparser/AppTest.java"
    );

    println("Compiling tests...");

    if (compilecode != 0)
    {
      System.err.println("Test compilation error");
      System.exit(1);
    }

    println("Running tests...");

    ProcessBuilder processBuilder = new ProcessBuilder (
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

  private static String getTestClasspath()
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

  public static void println(String str)
  {
    System.out.println(str);
  }
}
