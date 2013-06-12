package co.uk.codebreeze.testassembly.modulea;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;


public class ModuleA {
  //statically initialized at start up
  private final static String MODULE_A_CONFIG = System.getProperty("env.name") + "/" + "module-a.properties";

  public static final void main(final String...args) throws ConfigurationException {
    final Configuration config = new PropertiesConfiguration(MODULE_A_CONFIG);
  }
}
