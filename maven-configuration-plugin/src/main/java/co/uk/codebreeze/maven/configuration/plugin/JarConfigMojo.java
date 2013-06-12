package co.uk.codebreeze.maven.configuration.plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

/*
 * heavily influenced by maven jar Mojo
 **/
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class JarConfigMojo extends AbstractMojo {

    private static final String[] DEFAULT_EXCLUDES = new String[]{"**/package.html"};
    private static final String[] DEFAULT_INCLUDES = new String[]{"**/**"};
    /**
     * List of files to include. Specified as fileset patterns which are
     * relative to the input directory whose contents is being packaged into the
     * JAR.
     */
    @Parameter
    private String[] includes;
    /**
     * List of files to exclude. Specified as fileset patterns which are
     * relative to the input directory whose contents is being packaged into the
     * JAR.
     */
    @Parameter
    private String[] excludes;
    /**
     * Directory containing the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File outputDirectory;
    @Parameter(defaultValue = "${project.build.directory}/config", required = true)
    private File inputDirectory;
    /**
     * Name of the generated JAR.
     */
    @Parameter(
            alias = "jarName",
            required = true,
            property = "jar.finalName",
            defaultValue = "${project.build.finalName}")
    private String finalName;
    /**
     * The Jar archiver.
     */
    @Component(role = org.codehaus.plexus.archiver.Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;
    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    /**
     * The archive configuration to use. See <a
     * href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();
    /**
     * Path to the default MANIFEST file to use. It will be used if
     * <code>useDefaultManifestFile</code> is set to
     * <code>true</code>.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", required = true, readonly = true)
    private File defaultManifestFile;
    /**
     * Set this to
     * <code>true</code> to enable the use of the
     * <code>defaultManifestFile</code>.
     */
    @Parameter(defaultValue = "false", property = "jar.useDefaultManifestFile")
    private boolean useDefaultManifestFile;
    @Component
    private MavenProjectHelper projectHelper;
    /**
     * Whether creating the archive should be forced.
     */
    @Parameter(defaultValue = "false", property = "jar.forceCreation")
    private boolean forceCreation;
    /**
     * Skip creating empty archives
     */
    @Parameter(defaultValue = "false", property = "jar.skipIfEmpty")
    private boolean skipIfEmpty;
    
    @Parameter(required = true, defaultValue = "-config")
    private String classifierPostfix;
    
    @Parameter(required = false, defaultValue = "-")
    private String classifierPrefix;

    /**
     * Generates the JAR.
     */
    public File createArchive(final File environmentConfigDir, final String classifier)
            throws MojoExecutionException {
        final File jarFile = new File(outputDirectory, finalName + classifier + ".jar");
        final MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(jarFile);
        archive.setForced(forceCreation);
        archiver.getArchiver().addDirectory(environmentConfigDir, getIncludes(), getExcludes());
        final File existingManifest = defaultManifestFile;

        if (useDefaultManifestFile && existingManifest.exists() && archive.getManifestFile() == null) {
            getLog().info("Adding existing MANIFEST to archive. Found under: " + existingManifest.getPath());
            archive.setManifestFile(existingManifest);
        }
        
        try {
            archiver.createArchive(session, project, archive);
            return jarFile;
        } catch (ArchiverException ex) {
            throw new RuntimeException(ex);
        } catch (ManifestException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (DependencyResolutionRequiredException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Generates the JAR.
     */
    @Override
    public void execute()
            throws MojoExecutionException {
        final Map<String, File> environmentToConfigDirMap = extractEnvironmentsAndConfigFolderPaths(inputDirectory);
        for (final Map.Entry<String, File> environment : environmentToConfigDirMap.entrySet()) {
            final String classifier = classifierPrefix + environment.getKey() + classifierPostfix;
            final File jarFile = createArchive(environment.getValue(), classifier);
            projectHelper.attachArtifact(project, "jar", 
                    classifier.startsWith("-") ? classifier.substring(1) : classifier, jarFile);
        }
    }

    private String[] getIncludes() {
        if (includes != null && includes.length > 0) {
            return includes;
        }
        return DEFAULT_INCLUDES;
    }

    private String[] getExcludes() {
        if (excludes != null && excludes.length > 0) {
            return excludes;
        }
        return DEFAULT_EXCLUDES;
    }

    private Map<String, File> extractEnvironmentsAndConfigFolderPaths(final File configBaseDir) {
        final Map<String, File> environmentsToConfigBase = new HashMap<String, File>();
        final File[] listedContents = configBaseDir.listFiles();
        for (final File file : listedContents) {
            if (file.isDirectory()) {
                environmentsToConfigBase.put(file.getName(), file);
            } else {
                getLog().warn(String.format("ignoring non-environment file [%s]", file.getPath()));
            }
        }
        getLog().info(String.format("environment config dirs found [%s]", environmentsToConfigBase));
        return environmentsToConfigBase;
    }
}