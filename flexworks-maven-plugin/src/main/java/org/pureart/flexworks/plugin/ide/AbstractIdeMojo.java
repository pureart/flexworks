package org.pureart.flexworks.plugin.ide;

import static org.sonatype.flexmojos.plugin.common.FlexExtension.AIR;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWC;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWF;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.eclipse.EclipsePlugin;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.sonatype.flexmojos.util.PathUtil;

/**
 * @author pureart.org
 */
public class AbstractIdeMojo extends EclipsePlugin {

	protected static final String M2ECLIPSE_NATURE = "org.eclipse.m2e.core.maven2Nature";
	protected static final String M2ECLIPSE_BUILD_COMMAND = "org.eclipse.m2e.core.maven2Builder";

	/**
	 * List of path elements that form the roots of ActionScript class
	 * hierarchies.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;sourcePaths&gt;
	 *    &lt;path&gt;${baseDir}/src/main/flex&lt;/path&gt;
	 * &lt;/sourcePaths&gt;
	 * </pre>
	 * 
	 * By default use Maven source and resources folders.
	 * 
	 * @parameter
	 */
	protected File[] sourcePaths;

	/**
	 * Specifies the locale for internationalization
	 * <p>
	 * Equivalent to -compiler.locale
	 * </p>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;localesCompiled&gt;
	 *   &lt;locale&gt;en_US&lt;/locale&gt;
	 * &lt;/localesCompiled&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] localesCompiled;

	/**
	 * Relative path where the locales should be created
	 * 
	 * @parameter expression="${flex.localesOutputPath}"
	 */
	protected String localesOutputPath;

	/**
	 * Define the base path to locate resouce bundle files Accept some special
	 * tokens:
	 * 
	 * <pre>
	 * {locale}     - replace by locale name
	 * </pre>
	 * 
	 * @parameter default-value="${basedir}/src/main/locales/{locale}"
	 */
	protected File localesSourcePath;

	/**
	 * Specifies the locales for external internationalization bundles
	 * <p>
	 * No equivalent parameter
	 * </p>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;localesRuntime&gt;
	 *   &lt;locale&gt;en_US&lt;/locale&gt;
	 * &lt;/localesRuntime&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] localesRuntime;

	/**
	 * Tells if the project type is valid for swc,swf,air etc.
	 * 
	 * @return true if the project type valid is, false otherwise.
	 */
	protected boolean isValidProjectType() {
		return SWF.equals(packaging) || SWC.equals(packaging) || AIR.equals(packaging);
	}

	@Override
	protected void writeConfigurationExtras(EclipseWriterConfig config) throws MojoExecutionException {
		super.writeConfigurationExtras(config);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void fillDefaultNatures(String packaging) {
		super.fillDefaultNatures(packaging);
		getProjectnatures().add(M2ECLIPSE_NATURE);
	}

	@Override
	protected void fillDefaultBuilders(String packaging) {
		super.fillDefaultBuilders(packaging);
		// getBuildcommands().add(M2ECLIPSE_BUILD_COMMAND);
	}

	@Override
	protected void validateExtras() throws MojoExecutionException {
		super.validateExtras();

		if (!isValidProjectType()) {
			throw new MojoExecutionException("Invalid packaging type for!");
		}
	}

	@Override
	protected void setupExtras() throws MojoExecutionException {
		super.setupExtras();
		if (isValidProjectType()) {
			final File basedir = getProject().getBasedir();
			final File projectFile = new File(basedir, ".project");
			if (projectFile.exists())
				projectFile.delete();
		}
	}

	protected String plain(Collection<String> strings) {
		StringBuilder buf = new StringBuilder();
		for (String string : strings) {
			if (buf.length() != 0) {
				buf.append(' ');
			}
			buf.append(string);
		}
		return buf.toString();
	}

	@SuppressWarnings("unchecked")
	Collection<String> getSourceRoots() {
		if (sourcePaths != null) {
			return getAbsolutePaths(sourcePaths);
		}

		Set<String> sources = new HashSet<String>();
		List<String> sourceRoots;

		if (project.getExecutionProject() != null) {
			sourceRoots = project.getExecutionProject().getCompileSourceRoots();
		} else {
			sourceRoots = project.getCompileSourceRoots();
		}
		sources.addAll(sourceRoots);

		List<String> testRoots;
		if (project.getExecutionProject() != null) {
			testRoots = project.getExecutionProject().getTestCompileSourceRoots();
		} else {
			testRoots = project.getTestCompileSourceRoots();
		}
		sources.addAll(testRoots);

		for (Resource resource : (List<Resource>) project.getBuild().getResources()) {
			sources.add(resource.getDirectory());
		}
		for (Resource resource : (List<Resource>) project.getBuild().getTestResources()) {
			sources.add(resource.getDirectory());
		}

		for (Iterator<String> iterator = sources.iterator(); iterator.hasNext();) {
			String path = iterator.next();
			if (!new File(path).exists()) {
				iterator.remove();
			}
		}

		if (localesCompiled != null || localesRuntime != null) {
			// sources.add(resourceBundlePath);
			sources.add(PathUtil.relativePath(project.getBasedir(), localesSourcePath));
		}

		return sources;
	}

	private Collection<String> getAbsolutePaths(File[] sourcePaths) {
		Collection<String> paths = new HashSet<String>();
		for (File file : sourcePaths) {
			paths.add(file.getAbsolutePath());
		}
		return paths;
	}

	protected Collection<String> getRelativeSources() {
		Collection<String> sourceRoots = getSourceRoots();

		Collection<String> sources = new HashSet<String>();
		for (String sourceRoot : sourceRoots) {
			File source = new File(sourceRoot);
			if (source.isAbsolute()) {
				String relative = PathUtil.relativePath(project.getBasedir(), source);
				sources.add(relative.replace('\\', '/'));
			} else {
				sources.add(sourceRoot);
			}
		}

		return sources;
	}
}
