package co.uk.codebreeze.testassembly.modulea;

import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

public class ModuleATest {
  @Test
  public void printClassPathTest(){
    printClassPath();
  }

  public static void printClassPath() {
    final ClassLoader cl = ClassLoader.getSystemClassLoader();
    final URL[] urls = ((URLClassLoader)cl).getURLs();
    for(final URL url: urls){
      System.out.println(url.getFile());
    }
  }
}
