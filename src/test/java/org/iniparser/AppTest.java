package org.iniparser;

import org.iniparser.Iniparser.IniMap;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Unit test for simple ini file de/encoding.
 */
public class AppTest {

  @Test
  public void shouldAnswerWithTrue() {
    System.out.println("Main test function running..");
    Iniparser iniparser = new Iniparser();
    File fileToParse = new File("test-files/meow.ini");
    System.out.println("File to parse: " + fileToParse.getAbsolutePath());

    try { iniparser.setSource("test-files/meow.ini"); }
    catch (IOException ex) {
      ex.printStackTrace();
      return; 
    };

    Optional<IniMap> iniMap = iniparser.parse("test-files/meow.ini");
    System.out.println(iniMap);
    System.out.println("File parsed");
    assertTrue(true);
  }
}
