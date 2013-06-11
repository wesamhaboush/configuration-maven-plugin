configuration-maven-plugin
==========================

generate and deploy configuration files for different environments for maven projects. For example, if you have dev, qa, staging, and prod environments that you want to deploy different configurations for in a maven pom module WildSoftware, then you'd be able to produce the following artifacts:

      WildSoftware-1.1.jar
      WildSoftware-1.1-dev-config.jar
      WildSoftware-1.1-qa-config.jar
      WildSoftware-1.1-staging-config.jar
      WildSoftware-1.1-prod-config.jar


They also will all be deployed as part of the vanilla `deploy` phase without any special maven configuration.

# Goals

* `configuration:generate`
       generates filtered resources for all environment directories in the resourcesDir
* `configuration:package`
       jars the generated environment folders into jars with names incorporating various parameters

# Usage

For the generate goal:

* configure the location of the filters dir, default is `src/main/filters`
* configure the location of the resource dir, default is `src/main/resources`
* configure the location of the output config dir, default is `target/config`
* configure the properties that are allowed to be missing, default is none

For the package goal:

* configure the location of the configuration directories, default `target/config`
* configure the location where the jars need to be placed, default is `target/`

# Examples

A simple minimalist example is here:

               <plugin>
                  <groupId>net.codebreeze</groupId>
                  <artifactId>configuration-maven-plugin</artifactId>
                  <version>1.0-SNAPSHOT</version>
                  <executions>
                      <execution>
                          <id>process-configuration-directories</id>
                          <phase>process-resources</phase>
                          <goals>
                              <goal>generate</goal>
                          </goals>
                          <configuration>
                              <resourcesDir>src/main/templates</resourcesDir>
                              <filtersDir>src/main/config</filtersDir>
                              <includes>
                                  <include>**/*.properties</include>
                              </includes>
                              <allowedMissingProperties>
                                  <allowedMissingProperty>catalina.home</allowedMissingProperty>
                              </allowedMissingProperties>
                          </configuration>
                      </execution>
                      <execution>
                          <id>jar-configuration-artifacts</id>
                          <phase>package</phase>
                          <goals>
                              <goal>package</goal>
                          </goals>
                      </execution>
                  </executions>
              </plugin>

# Notes

This project has also 4 other sample ways to achieve something similar using various other mechanisms from the maven world:

* Using Ant
* Using Maven Profiles
* Using Assembly Descriptors
* Using Maven Classifiers
* Using Environment Variables

Try all of them before deciding what to go with.
