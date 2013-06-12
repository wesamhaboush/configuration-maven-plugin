package co.uk.codebreeze.maven.configuration.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GenerateConfigMojo extends AbstractMojo {
//    private final static Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[a-zA-Z]+\\w*\\}");

    private final static Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
    @Parameter(defaultValue = "src/main/resources")
    private String resourcesDir;
    /**
     * map from environment name (dev, prod, etc) to directories where the
     * properties file exist: db.properties, msging.properties, etc.
     */
//    @Parameter
//    private Map<String, String> environments;
    /**
     * where to put the output folders with the filtered files per environment
     */
    @Parameter(defaultValue = "src/main/filters")
    private String filtersDir;
    /**
     * where to put the output folders with the filtered files per environment
     */
    @Parameter(defaultValue = "${project.build.directory}/config")
    private String outputDirectory;
    @Parameter
    private List<String> includes = new ArrayList<String>();
    @Parameter
    private List<String> excludes = new ArrayList<String>();
    @Parameter(defaultValue = "true")
    private boolean includeUnfilteredResources;
    @Parameter(defaultValue = "true")
    private boolean failOnMissingProperties;
    @Parameter
    private String[] allowedMissingProperties = {};

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<String> resourceFiles = extractResourceFiles();
        final Map<String, File> environmentsMap = extractEnvironmentsAndFilterPaths(filtersDir);
        processEnvironments(resourceFiles, environmentsMap);
        if (includeUnfilteredResources) {
            try {
                FileUtils.copyDirectory(new File(resourcesDir), new File(outputDirectory, "templates"));
            } catch (IOException ex) {
                throw new RuntimeException("couldn't copy resources from " + resourcesDir + " to " + outputDirectory, ex);
            }
        }
    }

    private static DirectoryScanner scanner(final String[] includes,
            final String[] excludes,
            final String baseDir,
            final boolean isCasesensitive) {
        DirectoryScanner ds = new DirectoryScanner();
        ds.setIncludes(includes);
        ds.setExcludes(excludes);
        ds.setBasedir(baseDir);
        ds.setCaseSensitive(isCasesensitive);
        return ds;
    }

    private void processEnvironment(final String environment, final File filtersBaseDir, final List<String> filterFiles, final List<String> resourceFiles) throws IOException {
        final Map<String, String> properties = extractProperties(filtersBaseDir, filterFiles);
        getLog().info("properties extract for " + environment + " are : " + properties);
        final File environmentOutputDir = new File(outputDirectory, environment);
        environmentOutputDir.mkdirs();
        getLog().info("created path: " + environmentOutputDir);
        for (final String resource : resourceFiles) {
            filterResource(environmentOutputDir, resource, properties);
        }
    }

    private Map<String, String> extractProperties(final File filtersBaseDir, final List<String> filterFiles) throws IOException {
        final Map<String, String> propertyMap = new HashMap<String, String>();
        for (final String file : filterFiles) {
            getLog().info("reading value from: " + file);
            Properties properties = new Properties();
            properties.load(FileUtils.openInputStream(new File(filtersBaseDir, file)));
            for (final String propertyName : properties.stringPropertyNames()) {
                if (propertyMap.containsKey(propertyName)) {
                    throw new IllegalStateException("already found property " + propertyName + ", cannot have it twice");
                } else {
                    propertyMap.put(propertyName, properties.getProperty(propertyName));
                }
            }
        }
        getLog().info("final filter map: " + propertyMap);
        return propertyMap;
    }

    private void filterResource(
            final File environmentOutputDir,
            final String resource,
            final Map<String, String> properties) throws IOException {
        final File sourceFile = new File(resourcesDir, resource);
        getLog().info("processing template file " + sourceFile.getAbsolutePath());
        final String originalContent = FileUtils.readFileToString(sourceFile);
        final String filteredContent = StrSubstitutor.replace(originalContent, properties);
        final File targetFile = new File(environmentOutputDir, resource);
        if (failOnMissingProperties && VARIABLE_PATTERN.matcher(filteredContent).find()) {
            final Set<String> variables = findVariables(filteredContent);
            if (!Arrays.asList(allowedMissingProperties).containsAll(variables)) {
                throw new IllegalStateException(String.format(
                        "%nfound unfiltered properties [%s]:%n"
                        + "envDir[%s],%n"
                        + "resource[%s],%n"
                        + "originalContent[-----%n%s%n-----],%n"
                        + "filteredContent[-----%n%s%n-----],%n",
                        variables,
                        environmentOutputDir,
                        resource,
                        originalContent,
                        filteredContent));
            }
        }
        getLog().info("writing filtered file " + targetFile.getAbsolutePath());
        FileUtils.write(targetFile, filteredContent, Charset.forName("utf-8"));
    }

    private void processEnvironments(final List<String> resourceFiles, final Map<String, File> environmentsMap) throws RuntimeException {
        for (final Map.Entry<String, File> entry : environmentsMap.entrySet()) {
            final String environmentName = entry.getKey();
            getLog().info("process environment:" + environmentName);
            final DirectoryScanner filtersScanner = scanner(
                    new String[]{"**/*.properties"},
                    new String[]{},
                    entry.getValue().getAbsolutePath(),
                    false);
            filtersScanner.scan();
            final List<String> filterFiles = Arrays.asList(filtersScanner.getIncludedFiles());
            getLog().info(environmentName + " filterFiles for : " + filterFiles);
            try {
                processEnvironment(environmentName, entry.getValue(), filterFiles, resourceFiles);
            } catch (IOException ex) {
                throw new RuntimeException("failed to process environment " + environmentName, ex);
            }
        }
    }

    private List<String> extractResourceFiles() throws IllegalStateException {
        final DirectoryScanner resourcesScanner = scanner(
                includes.toArray(new String[0]),
                excludes.toArray(new String[0]),
                resourcesDir,
                true);
        resourcesScanner.scan();
        final List<String> resourceFiles = Arrays.asList(resourcesScanner.getIncludedFiles());
        getLog().info("resourceFiles: " + Arrays.asList(resourceFiles));
        return resourceFiles;
    }

    private Map<String, File> extractEnvironmentsAndFilterPaths(final String filtersDir) {
        final Map<String, File> environmentsToFilterPaths = new HashMap<String, File>();
        final File[] listedContents = new File(filtersDir).listFiles();
        for (final File file : listedContents) {
            if (file.isDirectory()) {
                environmentsToFilterPaths.put(file.getName(), file);
            } else {
                getLog().warn(String.format("ignoring non environment file [%s]", file.getPath()));
            }
        }
        getLog().info(String.format("environments found [%s]", environmentsToFilterPaths));
        return environmentsToFilterPaths;
    }

    public static Set<String> findVariables(final String targetText) {
        final Set<String> vars = new TreeSet<String>();
        final Matcher m = VARIABLE_PATTERN.matcher(targetText);
        while (m.find()) {
            int n = 0;
            for (int i = m.start() - 1; i >= 0 && targetText.charAt(i) == '\\'; i--) {
                n++;
            }
            if (n % 2 != 0) {
                continue;
            }
            final String var = targetText.substring(m.start() + 2, m.end() - 1);
            vars.add(var);
        }
        return vars;
    }
}
