package org.pureart.flexworks.plugin.ide;

import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.allOf;
import static org.sonatype.flexmojos.compatibilitykit.VersionUtils.isMinVersionOK;
import static org.sonatype.flexmojos.compatibilitykit.VersionUtils.splitVersion;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.AIR;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.RB_SWC;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWC;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWF;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.FileSet;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.eclipse.writers.EclipseWriterConfig;
import org.apache.maven.plugin.ide.IdeDependency;
import org.apache.maven.plugin.ide.IdeUtils;
import org.apache.maven.repository.RepositorySystem;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.hamcrest.Matcher;
import org.pureart.flexworks.plugin.ide.sdk.LinkType;
import org.pureart.flexworks.plugin.ide.sdk.LocalSdk;
import org.pureart.flexworks.plugin.ide.sdk.LocalSdkEntry;
import org.pureart.flexworks.plugin.util.ArtifactsUtils;
import org.sonatype.flexmojos.compatibilitykit.FlexMojo;
import org.sonatype.flexmojos.plugin.common.FlexScopes;
import org.sonatype.flexmojos.plugin.compiler.attributes.MavenNamespace;
import org.sonatype.flexmojos.plugin.utilities.SourceFileResolver;
import org.sonatype.flexmojos.util.PathUtil;

/**
 * 
 * @author pureart
 */
public class FlexBuilderMojo extends AbstractIdeMojo implements FlexMojo {

	static final String APPLICATION_NATURE = "com.adobe.flexbuilder.project.flexnature";
	static final String LIBRARY_NATURE = "com.adobe.flexbuilder.project.flexlibnature";
	static final String ACTIONSCRIPT_NATURE = "com.adobe.flexbuilder.project.actionscriptnature";
	static final String FLEXBUILDER_AIR_NATURE = "com.adobe.flexbuilder.apollo.apollonature";
	static final String FLEXBUILDER_BUILD_COMMAND = "com.adobe.flexbuilder.project.flexbuilder";
	static final String AIR_BUILD_COMMAND = "com.adobe.flexbuilder.apollo.apollobuilder";

	/**
	 * Tells wethter extract the RSL file to SWF by auto.
	 * 
	 * @parameter default-value="false"
	 */
	protected boolean autoExtractRSL;

	/**
	 * Additional application files. The paths must be relative to the source
	 * folder.
	 * 
	 * @parameter
	 * @alias "applications"
	 */
	protected List<String> additionalApplications;

	/**
	 * List of css files that will be compiled into swfs within Eclipse. The
	 * path must be relative to the base directory of the project. Usage:
	 * 
	 * <pre>
	 * &lt;buildCssFiles&amp;gt
	 *     &lt;path&gt;src/style/main.css&lt;path&gt;
	 * &lt;/buildCssFiles&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] buildCssFiles;

	/**
	 * @parameter default-value="true"
	 *            expression="${enableFlexBuilderBuildCommand}"
	 */
	protected boolean enableFlexBuilderBuildCommand;

	/**
	 * @parameter default-value="false" expression="${generateHtmlWrapper}"
	 */
	protected boolean generateHtmlWrapper;

	/**
	 * Customize the outputFolderPath of the Eclipse FlexBuilder/FlashBuilder
	 * Compiler.
	 * 
	 * @parameter default-value="bin-debug"
	 */
	protected String ideOutputFolderPath;

	/**
	 * Customize the output folder URL of the project.
	 * 
	 * @parameter
	 */
	protected String rootURL;

	/**
	 * Directory path where the html template files will be copied to. Since
	 * Flex Builder is hard coded to ${basedir}/html-template there should be no
	 * reason to change this.
	 * 
	 * @parameter default-value="${basedir}/html-template"
	 */
	protected File ideTemplateOutputFolder;

	/**
	 * Specifies whether this project should be treated as a Flex or a pure
	 * ActionScript project by Flexbuilder. If set to true:
	 * <ul>
	 * <li>Removes Flex (app/lib) natures from <code>.project</code> file.</li>
	 * <li>Changes exclusions from library path entries in
	 * <code>.actionScriptProperties</code> file.</li>
	 * <li>Completly omits creation of the <code>.flexProperties</code> file.</li>
	 * </ul>
	 * If not defined Flexmojos will lockup for
	 * com.adobe.flex.framework:framework:swc and set as <i>true</i> if found or
	 * <i>false</i> if not found
	 * 
	 * @parameter
	 */
	protected Boolean pureActionScriptProject;

	/* Start Duplicated */

	/**
	 * Turn on generation of accessible SWFs.
	 * 
	 * @parameter default-value="false"
	 */
	protected boolean accessible;

	/**
	 * This is equilvalent to the
	 * <code>compiler.mxmlc.compatibility-version</code> option of the compc
	 * compiler. Must be in the form <major>.<minor>.<revision> Valid values:
	 * <tt>2.0.0</tt>, <tt>2.0.1</tt> and <tt>3.0.0</tt>
	 * 
	 * @see http
	 *      ://livedocs.adobe.com/flex/3/html/help.html?content=versioning_4.
	 *      html
	 * @parameter
	 */
	protected String compatibilityVersion;

	/**
	 * Load a file containing configuration options If not defined, by default
	 * will search for one on resources folder.
	 * 
	 * @parameter
	 */
	protected List<File> configFiles;

	/**
	 * Context root to pass to the compiler.
	 * 
	 * @parameter default-value="."
	 */
	protected String contextRoot;

	/**
	 * Sets the default application width in pixels. This is equivalent to using
	 * the <code>default-size</code> option of the mxmlc or compc compilers.
	 * 
	 * @parameter default-value="500"
	 */
	protected int defaultSizeWidth;

	/**
	 * Sets the default application height in pixels. This is equivalent to
	 * using the <code>default-size</code> option of the mxmlc or compc
	 * compilers.
	 * 
	 * @parameter default-value="375"
	 */
	protected int defaultSizeHeight;

	/**
	 * defines: specifies a list of define directive key and value pairs. For
	 * example, CONFIG::debugging<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;definesDeclaration&gt;
	 *   &lt;property&gt;
	 *     &lt;name&gt;SOMETHING::aNumber&lt;/name&gt;
	 *     &lt;value&gt;2.2&lt;/value&gt;
	 *   &lt;/property&gt;
	 *   &lt;property&gt;
	 *     &lt;name&gt;SOMETHING::aString&lt;/name&gt;
	 *     &lt;value&gt;&quot;text&quot;&lt;/value&gt;
	 *   &lt;/property&gt;
	 * &lt;/definesDeclaration&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected Properties definesDeclaration;

	/**
	 * Keep the following AS3 metadata in the bytecodes.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;keepAs3Metadatas&gt;
	 *   &lt;keepAs3Metadata&gt;Bindable&lt;/keepAs3Metadata&gt;
	 *   &lt;keepAs3Metadata&gt;Events&lt;/keepAs3Metadata&gt;
	 * &lt;/keepAs3Metadatas&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] keepAs3Metadatas;

	/**
	 * Default locale for libraries. This is useful to non localized
	 * applications, just to define swc.rb locale
	 * 
	 * @parameter default-value="en_US"
	 */
	protected String defaultLocale;

	/**
	 * The greeting to display.
	 * 
	 * @parameter services default-value="true"
	 */
	protected boolean incremental;

	/**
	 * The maven resources
	 * 
	 * @parameter expression="${project.build.resources}"
	 * @required
	 * @readonly
	 */
	protected List<Resource> resources;

	/**
	 * Automatically scans the paths looking for compile units (.as and .mxml
	 * files) adding the represented classes with the
	 * <code>include-classes</code> option.
	 * <p>
	 * This option is useful if you want to compile as with
	 * <code>includeClasses</code> option without the need to manually maintain
	 * the class list in the pom.
	 * </p>
	 * <p>
	 * Specify <code>includes</code> parameter to include different compile
	 * units than the default .as and .mxml ones. Specify <code>excludes</code>
	 * parameter to exclude compile units that would otherwise be included.
	 * </p>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;includeAsClasses&gt;
	 *   &lt;sources&gt;
	 *     &lt;directory&gt;${baseDir}/src/main/flex&lt;/directory&gt;
	 *     &lt;excludes&gt;
	 *       &lt;exclude&gt;&#042;&#042;/&#042;Incl.as&lt;/exclude&gt;
	 *     &lt;/excludes&gt;
	 *   &lt;/sources&gt;
	 * &lt;/includeAsClasses&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected FileSet[] includeAsClasses;

	/**
	 * This is the equilvalent of the <code>include-classes</code> option of the
	 * compc compiler.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;includeClassses&gt;
	 *   &lt;class&gt;foo.Bar&lt;/class&gt;
	 * &lt;/includeClasses&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] includeClasses;

	/**
	 * This is equilvalent to the <code>include-namespaces</code> option of the
	 * compc compiler.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;includeNamespaces&gt;
	 *   &lt;namespace&gt;http://www.adobe.com/2006/mxml&lt;/namespace&gt;
	 * &lt;/includeNamespaces&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] includeNamespaces;

	/**
	 * This is the equilvalent of the <code>include-sources</code> option of the
	 * compc compiler.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;includeSources&gt;
	 *   &lt;sources&gt;${baseDir}/src/main/flex&lt;/sources&gt;
	 * &lt;/includeSources&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected File[] includeSources;

	/**
	 * This is equivalent to the <code>include-file</code> option of the compc
	 * compiler.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;includeFiles&gt;
	 * &lt;file&gt;${baseDir}/anyFile.txt&lt;/file&gt;
	 * &lt;/includeFiles&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected File[] includeFiles;

	/**
	 * Instructs the compiler to keep a style sheet's type selector in a SWF
	 * file, even if that type (the class) is not used in the application. This
	 * is equivalent to using the <code>compiler.keep-all-type-selectors</code>
	 * option of the mxmlc or compc compilers.
	 * 
	 * @parameter default-value="false"
	 */
	protected boolean keepAllTypeSelectors;

	/**
	 * The list of modules files to be compiled. The path must be relative with
	 * source folder.<BR>
	 * This will create a modules entry in .actionScriptProperties Usage:
	 * 
	 * <pre>
	 * &lt;modules&gt;
	 *   &lt;module&gt;com/acme/AModule.mxml&lt;/module&gt;
	 * &lt;/modules&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	private String[] modules;

	/**
	 * Specify a URI to associate with a manifest of components for use as MXML
	 * elements.<BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;namespaces&gt;
	 *   &lt;namespace&gt;
	 *     &lt;uri&gt;http://www.adobe.com/2006/mxml&lt;/uri&gt;
	 *     &lt;manifest&gt;${basedir}/manifest.xml&lt;/manifest&gt;
	 *   &lt;/namespace&gt;
	 * &lt;/namespaces&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected MavenNamespace[] namespaces;

	/**
	 * policyFileUrls array of policy file URLs. Each entry in the rslUrls array
	 * must have a corresponding entry in this array. A policy file may be
	 * needed in order to allow the player to read an RSL from another domain.
	 * If a policy file is not required, then set it to an empty string. Accept
	 * some special tokens:
	 * 
	 * <pre>
	 * {contextRoot}        - replace by defined context root
	 * {groupId}            - replace by library groupId
	 * {artifactId}         - replace by library artifactId
	 * {version}            - replace by library version
	 * {extension}          - replace by library extension swf or swz
	 * </pre>
	 * 
	 * <BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;policyFileUrls&gt;
	 *   &lt;url&gt;/{contextRoot}/rsl/policy-{artifactId}-{version}.xml&lt;/url&gt;
	 * &lt;/policyFileUrls&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	private String[] policyFileUrls;

	/**
	 * rslUrls array of URLs. The first RSL URL in the list is the primary RSL.
	 * The remaining RSL URLs will only be loaded if the primary RSL fails to
	 * load. Accept some special tokens:
	 * 
	 * <pre>
	 * {contextRoot}        - replace by defined context root
	 * {groupId}            - replace by library groupId
	 * {artifactId}         - replace by library artifactId
	 * {version}            - replace by library version
	 * {extension}          - replace by library extension swf or swz
	 * </pre>
	 * 
	 * default-value="{contextRoot}/rsl/{artifactId}-{version}.{extension}" <BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;rslUrls&gt;
	 *   &lt;url&gt;{contextRoot}/rsl/{artifactId}-{version}.{extension}&lt;/url&gt;
	 * &lt;/rslUrls&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] rslUrls;

	/**
	 * Run the AS3 compiler in a mode that detects legal but potentially
	 * incorrect code
	 * 
	 * @parameter default-value="true"
	 */
	protected boolean showWarnings;

	/**
	 * The file to be compiled. The path must be relative to the source folder.
	 * 
	 * @parameter
	 */
	protected String sourceFile;

	/**
	 * Run the AS3 compiler in strict error checking mode.
	 * 
	 * @parameter default-value="true"
	 */
	protected boolean strict;

	/**
	 * specifies the version of the player the application is targeting.
	 * Features requiring a later version will not be compiled into the
	 * application. The minimum value supported is "9.0.0". If not defined will
	 * take the default value from current playerglobal dependency.
	 * 
	 * @parameter default-value="0.0.0"
	 */
	protected String targetPlayer;

	/**
	 * The template URI. This is the same usage as on the wrapper mojo. To make
	 * this mojo copy the template URI to the templateOutputPath
	 * generateHtmlWrapper must be set to true.
	 * <p>
	 * You can point to a zip file, a folder or use one of the following embed
	 * templates:
	 * <ul>
	 * embed:client-side-detection
	 * </ul>
	 * <ul>
	 * embed:client-side-detection-with-history
	 * </ul>
	 * <ul>
	 * embed:express-installation
	 * </ul>
	 * <ul>
	 * embed:express-installation-with-history
	 * </ul>
	 * <ul>
	 * embed:no-player-detection
	 * </ul>
	 * <ul>
	 * embed:no-player-detection-with-history
	 * </ul>
	 * To point to a zip file you must use a URI like this:
	 * 
	 * <pre>
	 * zip:/myTemplateFolder/template.zip
	 * zip:c:/myTemplateFolder/template.zip
	 * </pre>
	 * 
	 * To point to a folder use a URI like this:
	 * 
	 * <pre>
	 * folder:/myTemplateFolder/
	 * folder:c:/myTemplateFolder/
	 * </pre>
	 * <p>
	 * Unlike the html wrapper mojo this mojo will only copy the template files
	 * to the htmlTemplateOutputPath. From there Flex Builder will work with
	 * them as normal.
	 * 
	 * @parameter default-value="embed:express-installation-with-history"
	 */
	protected String templateURI;

	/**
	 * List of CSS or SWC files to apply as a theme. <BR>
	 * Usage:
	 * 
	 * <pre>
	 * &lt;themes&gt;
	 *    &lt;theme&gt;css/main.css&lt;/theme&gt;
	 * &lt;/themes&gt;
	 * </pre>
	 * 
	 * If you are using SWC theme should be better keep it's version controlled,
	 * so is advised to use a dependency with theme scope.<BR>
	 * Like this:
	 * 
	 * <pre>
	 * &lt;dependency&gt;
	 *   &lt;groupId&gt;com.adobe.flex.framework&lt;/groupId&gt;
	 *   &lt;artifactId&gt;spark/halo&lt;/artifactId&gt;
	 *   &lt;type&gt;css/swc&lt;/type&gt;
	 *   &lt;scope&gt;theme&lt;/scope&gt;
	 *   &lt;version&gt;flex.sdk.version&lt;/version&gt;
	 * &lt;/dependency&gt;
	 * </pre>
	 * 
	 * @parameter
	 */
	protected String[] themes;

	/**
	 * Sets the location of the Flex Data Services service configuration file.
	 * This is equivalent to using the <code>compiler.services</code> option of
	 * the mxmlc and compc compilers. If not define will look inside resources
	 * directory for services-config.xml
	 * 
	 * @parameter
	 */
	protected File services;

	/**
	 * @parameter default-value="true" expression="${debug}"
	 */
	protected boolean debug;

	/**
	 * Verifies the RSL loaded has the same digest as the RSL specified when the
	 * application was compiled. This is equivalent to using the
	 * <code>verify-digests</code> option in the mxmlc compiler.
	 * 
	 * @parameter default-value="true"
	 */
	protected boolean verifyDigests;

	/* End Duplicated */

	/**
	 * @parameter default-value="true"
	 */
	protected boolean htmlExpressInstall;

	/**
	 * @parameter default-value="true"
	 */
	protected boolean htmlHistoryManagement;

	/**
	 * @parameter default-value="true"
	 */
	protected boolean htmlPlayerVersionCheck;

	/* Internal Properties */

	/**
	 * LW : needed for expression evaluation Note : needs at least maven 2.0.8
	 * because of MNG-3062 The maven MojoExecution needed for
	 * ExpressionEvaluation
	 * 
	 * @parameter expression="${mojoExecution}"
	 * @required
	 * @readonly
	 */
	protected MojoExecution execution;

	protected IdeDependency globalDependency;

	/**
	 * @parameter expression="${plugin.artifacts}"
	 */
	protected List<Artifact> pluginArtifacts;

	/**
	 * @parameter expression="${project.remoteArtifactRepositories}"
	 */
	protected List<ArtifactRepository> remoteRepositories;

	/**
	 * @component
	 * @readonly
	 */
	protected RepositorySystem repositorySystem;

	/**
	 * LW : needed for expression evaluation The maven MojoExecution needed for
	 * ExpressionEvaluation
	 * 
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	protected MavenSession sessionContext;

	/**
	 * @component
	 */
	protected VelocityComponent velocityComponent;

	/**
	 * LocalSdk holder
	 */
	protected LocalSdk sdk;

	/**
	 * @parameter expression="${framework.path}"
	 */
	protected File frameworkPath;

	/**
	 * Current project type.
	 */
	public static ProjectType CURRENT_PROJECT_TYPE;

	@Override
	protected void writeConfigurationExtras(EclipseWriterConfig config) throws MojoExecutionException {
		super.writeConfigurationExtras(config);
		IdeDependency[] deps = config.getDeps();
		init();
		// Converted dependencies
		Collection<FbIdeDependency> dependencies = getConvertedDependencies(deps);

		// Get project type
		ProjectType type = CURRENT_PROJECT_TYPE = ProjectType.getProjectType(packaging, isUseApolloConfig(), pureActionScriptProject);

		// Initialize new Local SDK to help with the dependency cleaning
		// process.

		sdk = new LocalSdk(getCompilerVersion(), type, frameworkPath, getLog());

		if (type == ProjectType.FLEX || type == ProjectType.FLEX_LIBRARY || type == ProjectType.AIR
				|| type == ProjectType.AIR_LIBRARY || type == ProjectType.ACTIONSCRIPT) {
			dependencies = getCleanDependencies(dependencies, sdk);
			targetPlayer = getTargetPlayerVersion();
			writeFlexConfig(type, dependencies);
			writeAsProperties(type, dependencies);
		}

		if (type == ProjectType.FLEX)
			writeHtmlTemplate();
		if (type == ProjectType.FLEX || type == ProjectType.AIR)
			writeFlexProperties();
		if (type == ProjectType.FLEX_LIBRARY || type == ProjectType.AIR_LIBRARY)
			writeFlexLibProperties();
	}

	protected Collection<FbIdeDependency> resolveResourceBundles(Collection<FbIdeDependency> dependencies)
			throws MojoExecutionException {
		Collection<String> locales = getLocales();

		Collection<FbIdeDependency> extraRbs = new LinkedHashSet<FbIdeDependency>();

		Iterator<FbIdeDependency> it = dependencies.iterator();

		while (it.hasNext()) {
			IdeDependency dependency = it.next();
			// Ignore SWC dependencies
			if (SWC.equals(dependency.getType())) {
				continue;
			}
			// Convert resource beacons to fully qualified resource bundles for
			// the current locale set.
			else if (RB_SWC.equals(dependency.getType())) {
				for (String locale : locales) {
					String scope = getDependencyScope(dependency);

					Artifact art = repositorySystem.createArtifactWithClassifier(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(), dependency.getType(), locale);

					IdeDependency dep = new IdeDependency(art.getGroupId(), art.getArtifactId(), art.getVersion(), art.getClassifier(), false, Artifact.SCOPE_TEST.equals(art.getScope()), false, false, false, art.getFile(), art.getType(), false, null, 1, IdeUtils.getProjectName(IdeUtils.PROJECT_NAME_DEFAULT_TEMPLATE, art));

					// convert to FbIdeDependency to retain scope value.
					FbIdeDependency fbdep = new FbIdeDependency(dep, scope);
					if (null != fbdep.getFile())
						fbdep.setFile(new File(fbdep.getFile().getPath().replace(localRepository.getBasedir(), "${M2_REPO}")));
					extraRbs.add(fbdep);
				}
				it.remove();
			}
			// The dependency is unknown... just get rid of it.
			else {
				it.remove();
			}
		}

		dependencies.addAll(extraRbs);
		return dependencies;
	}

	protected Collection<FbIdeDependency> getCleanDependencies(Collection<FbIdeDependency> dependencies, LocalSdk sdk)
			throws MojoExecutionException {
		// Resolves and adds all resource bundles to the dependency collection.
		resolveResourceBundles(dependencies);

		Iterator<FbIdeDependency> iter = dependencies.iterator();
		// Loop through dependencies and perform any clean up necessary
		while (iter.hasNext()) {
			FbIdeDependency dependency = iter.next();

			// Toss out any dependency that is not understood
			if (!SWC.equals(dependency.getType()) && !RB_SWC.equals(dependency.getType())) {
				iter.remove();
				continue;
			}

			// Configure dependency path and dependency source path
			if (dependency.isReferencedProject()) {
				String projectName = dependency.getEclipseProjectName();
				// /todolist-lib/bin-debug/todolist-lib.swc
				dependency.setPath("/" + projectName + "/bin-debug/" + projectName + ".swc");
				dependency.setSourcePath("/" + projectName + "/src/main/flex/");
			} else {
				if (null != dependency.getFile())
					dependency.setPath(dependency.getFile().getPath().replace(localRepository.getBasedir(), "${M2_REPO}"));
			}
		}

		return dependencies;
	}

	private void init() throws MojoExecutionException {
		if (services == null) {
			@SuppressWarnings("unchecked")
			final List<Resource> resources = (List<Resource>) project.getBuild().getResources();
			for (Resource resource : resources) {
				File cfg = new File(resource.getDirectory(), "services-config.xml");
				if (cfg.exists()) {
					services = cfg;
					break;
				}
			}
		}

		if (definesDeclaration != null) {
			resolveDefinesDeclaration();
		}
	}

	private Collection<FbIdeDependency> getConvertedDependencies(IdeDependency[] dependencies) {
		List<FbIdeDependency> fbDeps = new ArrayList<FbIdeDependency>();

		for (IdeDependency dep : dependencies) {

			// Include only swc and rb.swc types
			if (SWC.equals(dep.getType()) || RB_SWC.equals(dep.getType())) {
				FbIdeDependency fbDep = new FbIdeDependency(dep, getDependencyScope(dep));

				// Set RSL URL template if the scope is either RSL or CACHING
				if (FlexScopes.RSL.equals(fbDep.getScope()) || FlexScopes.CACHING.equals(fbDep.getScope())) {
					// NOTE artifactId, version and extension are all replaced
					// when getRslUrl is called on the artifact
					String rslTemplate = (rslUrls != null && rslUrls.length > 0) ? rslUrls[0]
							: "{contextRoot}/rsl/{artifactId}-{version}.{extension}";
					rslTemplate = StringUtils.replace(rslTemplate, "{contextRoot}", contextRoot);
					fbDep.setRslUrl(rslTemplate);

					String policyFileUrl = (policyFileUrls != null && policyFileUrls.length > 0) ? policyFileUrls[0]
							: "";
					policyFileUrl = StringUtils.replace(policyFileUrl, "{contextRoot}", contextRoot);
					fbDep.setPolicyFileUrl(policyFileUrl);
				}

				// Save reference to player global dependencies for later use
				if (("playerglobal".equals(fbDep.getArtifactId()) || "airglobal".equals(fbDep.getArtifactId()))
						&& SWC.equals(fbDep.getType())) {
					// ignore playerglobal or airglobal that is scoped as test.
					// these are picked up by test dependencies so need to be
					// filtered out.
					if (fbDep.getScope().equals("test"))
						continue;

					// Make sure global artifact is scope external.
					fbDep.setScope(FlexScopes.EXTERNAL);

					globalDependency = fbDep;
				}

				fbDeps.add(fbDep);
			}
		}

		return fbDeps;
	}

	private String getDependencyScope(IdeDependency ideDependency) {
		final Set<Artifact> artifacts = project.getArtifacts();

		Artifact artifact = null;

		if (getLog().isDebugEnabled()) {
			getLog().debug(String.format("Searching for artifact matching IDE Depependecy %s:%s:%s:%s", ideDependency.getGroupId(), ideDependency.getArtifactId(), ideDependency.getVersion(), ideDependency.getType()));
		}

		for (Iterator<Artifact> it = artifacts.iterator(); it.hasNext();) {
			artifact = it.next();

			if (getLog().isDebugEnabled()) {
				getLog().debug(String.format("Checking artifact %s:%s:%s:%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getType()));
			}

			// match referenced projects
			if (ideDependency.isReferencedProject()) {
				if (ideDependency.getGroupId().equals(artifact.getGroupId())
						&& ideDependency.getArtifactId().equals(artifact.getArtifactId()))
					break; // match found
			}
			// match non referenced projects using files paths to avoid problems
			// with SNAPSHOT version matching.
			else if (artifact.getFile().equals(ideDependency.getFile())) {
				// match classifiers if needed.
				if (ideDependency.getClassifier() != null) {
					if (ideDependency.getClassifier().equals(artifact.getClassifier()))
						break; // match found
				} else
					break; // match found
			}

			// artifact did not match. null and continue loop
			artifact = null;
		}

		if (artifact == null)
			getLog().warn("Unable to find artifact for IDE dependency! " + ideDependency);

		String scope = null;
		if (artifact != null)
			scope = artifact.getScope();

		return scope;
	}

	protected void resolveDefinesDeclaration() throws MojoExecutionException {

		Properties clean = new Properties();

		ExpressionEvaluator expressionEvaluator = null;
		expressionEvaluator = new PluginParameterExpressionEvaluator(sessionContext, execution);

		for (Object defineKey : definesDeclaration.keySet()) {
			String defineName = defineKey.toString();
			String value = definesDeclaration.getProperty(defineName);
			if (value.contains("${")) {
				try {
					value = (String) expressionEvaluator.evaluate(value);
				} catch (ExpressionEvaluationException e) {
					throw new MojoExecutionException("Expression error in " + defineName, e);
				}
			}
			clean.put(defineName, value);
		}
		definesDeclaration = clean;
	}

	@Override
	protected void setupExtras() throws MojoExecutionException {
		super.setupExtras();
		final Set<Artifact> artifacts = project.getArtifacts();
		if (pureActionScriptProject == null) {
			pureActionScriptProject = ArtifactsUtils.searchFor(artifacts, "com.adobe.flex.framework", "framework", null, "swc", null) == null
					&& ArtifactsUtils.searchFor(artifacts, "com.adobe.flex.framework", "flex-framework", null, "pom", null) == null;
		}
		getLog().info("It's" + (pureActionScriptProject ? "" : " not") + " a pure actionscript project...");

		// validate IDE output directory.
		if (ideOutputFolderPath != null && !ideOutputFolderPath.isEmpty()) {
			final File outputFile = new File(ideOutputFolderPath);
			if (!outputFile.exists()) {
				outputFile.mkdirs();
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void fillDefaultNatures(String packaging) {
		super.fillDefaultNatures(packaging);
		if (SWF.equals(packaging)) {
			if (!pureActionScriptProject) {
				// only add flex-app nature if this is not a pure AS project
				getProjectnatures().add(APPLICATION_NATURE);
			}
			getProjectnatures().add(ACTIONSCRIPT_NATURE);
		} else if (SWC.equals(packaging)) {
			if (!pureActionScriptProject)
				getProjectnatures().add(LIBRARY_NATURE);
			getProjectnatures().add(ACTIONSCRIPT_NATURE);
		} else if (AIR.equals(packaging)) {
			getProjectnatures().add(APPLICATION_NATURE);
			getProjectnatures().add(ACTIONSCRIPT_NATURE);
			getProjectnatures().add(FLEXBUILDER_AIR_NATURE);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void fillDefaultBuilders(String packaging) {
		super.fillDefaultBuilders(packaging);
		if (enableFlexBuilderBuildCommand)
			if (SWF.equals(packaging) || SWC.equals(packaging) || AIR.equals(packaging))
				getBuildcommands().add(FLEXBUILDER_BUILD_COMMAND);
			else if (AIR.equals(packaging))
				getBuildcommands().add(AIR_BUILD_COMMAND);
	}

	@Override
	public String getCompilerVersion() {
		Artifact compiler = ArtifactsUtils.searchFor(project.getPluginArtifacts(), "com.adobe.flex", "compiler", null, "pom", null);
		if (compiler != null)
			return compiler.getVersion();
		return project.getProperties().getProperty("flex.sdk.version");
	}

	protected String getTargetPlayerVersion() throws MojoExecutionException {
		String version = null;

		IdeDependency globalArtifact = getGlobalArtifact();
		if (globalArtifact.getArtifactId().equals("airglobal")) {
			// not sure what to do here
			getLog().warn("Target player not set, not sure how to behave on air projects");
			return version;
		}

		String globalVersion = globalArtifact.getClassifier();
		int[] playerGlobalVersion;
		if (globalVersion == null) {
			// Older playerglobal artifacts had the version appended to the
			// artifact version.
			// Example: 9-3.2.0.3958
			if (globalArtifact.getVersion().contains("-")) {
				globalVersion = globalArtifact.getVersion().split("-")[0];
				if (globalVersion == null) {
					getLog().warn("Player global doesn't cointain classifier");
					return version;
				} else {
					playerGlobalVersion = splitVersion(globalVersion);
				}
			} else {
				getLog().warn("Player global doesn't cointain classifier");
				return version;
			}
		} else {
			playerGlobalVersion = splitVersion(globalVersion);
		}

		if (targetPlayer != null) {
			version = targetPlayer;
		} else {
			// target player version not specified so create it from the maven
			// artifact
			int[] tmpVersion = splitVersion(globalVersion, 3);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < tmpVersion.length; i++) {
				if (i > 0)
					sb.append(".");

				sb.append(tmpVersion[i]);
			}
			version = sb.toString();
		}

		int[] versions = splitVersion(version, 3);
		final boolean specific = Arrays.equals(versions, new int[] { 0, 0, 0 });
		if (!specific && versions[0] < 9) {
			throw new MojoExecutionException("Invalid target player version " + targetPlayer);
		}

		if (!isMinVersionOK(playerGlobalVersion, versions)) {
			throw new MojoExecutionException("TargetPlayer and playerglobal dependency version doesn't match! Target player: "
					+ targetPlayer + ", player global: " + globalVersion);
		}

		return version;
	}

	protected IdeDependency getGlobalArtifact() throws MojoExecutionException {
		if (globalDependency != null) {
			return globalDependency;
		}

		throw new MojoExecutionException("Player/Air Global dependency not found.");
	}

	private boolean isUseApolloConfig() {
		return "airglobal".equals(globalDependency.getArtifactId());
	}

	protected String getFlexConfigTemplate() {
		return "/templates/flexbuilder/flexConfig.vm";
	}

	private void writeFlexConfig(ProjectType type, Collection<FbIdeDependency> dependencies)
			throws MojoExecutionException {
		runVelocity(getFlexConfigTemplate(), ".flexConfig.xml", getFlexConfigContext(type, dependencies));
	}

	protected String getAsPropertiesTemplate() {
		return "/templates/flexbuilder/actionScriptProperties.vm";
	}

	private void writeAsProperties(ProjectType type, Collection<FbIdeDependency> dependecies)
			throws MojoExecutionException {
		runVelocity(getAsPropertiesTemplate(), ".actionScriptProperties", getAsPropertiesContext(type, dependecies));
	}

	/**
	 * Some compiler parameters don't work will or at all in the
	 * .actionScriptProperties. Rather than clutter up the
	 * additionalCompilerAreguments more lets just write stuff to a config file.
	 * 
	 * @param type
	 * @param ideDependencies
	 * @return
	 * @throws MojoExecutionException
	 */
	@SuppressWarnings("deprecation")
	protected VelocityContext getFlexConfigContext(ProjectType type, Collection<FbIdeDependency> ideDependencies)
			throws MojoExecutionException {
		VelocityContext context = new VelocityContext();

		context.put("namespaces", namespaces);

		if (definesDeclaration != null) {
			ExpressionEvaluator expressionEvaluator = new PluginParameterExpressionEvaluator(sessionContext, execution, null, null, project, project.getProperties());

			for (Object definekey : definesDeclaration.keySet()) {
				String defineName = definekey.toString();
				String value = definesDeclaration.getProperty(defineName);
				if (value.contains("${")) {
					// Fix bug in maven which doesn't always evaluate ${}
					// constructions
					try {
						value = (String) expressionEvaluator.evaluate(value);
					} catch (ExpressionEvaluationException e) {
						throw new MojoExecutionException("Expression error in " + defineName, e);
					}

					definesDeclaration.setProperty(defineName, value);
				}
			}

			context.put("defines", definesDeclaration);
		}

		context.put("metadatas", keepAs3Metadatas);

		if (SWF.equals(packaging) || AIR.equals(packaging)) {
			if (services != null)
				context.put("services", services.getAbsolutePath());

			if (compatibilityVersion != null)
				context.put("compatibilityVersion", compatibilityVersion);

			if (keepAllTypeSelectors)
				context.put("keepAllTypeSelectors", keepAllTypeSelectors);

			context.put("defaultSizeWidth", defaultSizeWidth);
			context.put("defaultSizeHeight", defaultSizeHeight);

			List<String> dependentThemes = getThemes(ideDependencies);
			context.put("themes", dependentThemes);

		}

		// Locales need to be available in SWC projects so merge them in.
		context.put("locales", getLocales());

		if (configFiles == null)
			configFiles = new ArrayList<File>();

		configFiles.add(new File(this.project.getBasedir() + "/.flexConfig.xml"));

		return context;
	}

	protected VelocityContext getAsPropertiesContext(ProjectType type, Collection<FbIdeDependency> dependencies)
			throws MojoExecutionException {
		File sourceDirectory = new File(project.getBuild().getSourceDirectory());

		VelocityContext context = new VelocityContext();
		// context.put("useM2Home", useM2Repo);
		context.put("dependencies", getNonSdkDependencies(dependencies));
		context.put("sdkExcludes", sdk.getExcludes(dependencies));
		context.put("sdkMods", getModifiedSdkDependencies(dependencies, sdk.getModified(dependencies)));
		context.put("mainSources", getMainSources());
		context.put("ideOutputFolderPath", ideOutputFolderPath);
		context.put("rootURL", rootURL);
		context.put("targetPlayer", targetPlayer);
		context.put("accessible", accessible);
		context.put("strict", strict);
		context.put("debug", debug);
		context.put("useApolloConfig", isUseApolloConfig());
		context.put("verifyDigests", verifyDigests);
		context.put("showWarnings", showWarnings);
		context.put("flexSDK", getFlexSdkVersion());
		context.put("htmlHistoryManagement", htmlHistoryManagement);
		context.put("htmlPlayerVersionCheck", htmlPlayerVersionCheck);
		context.put("htmlExpressInstall", htmlExpressInstall);

		StringBuilder additionalCompilerArguments = new StringBuilder();

		if (incremental) {
			additionalCompilerArguments.append(" --incremental ");
		}

		// if (contextRoot != null)
		// {
		// additionalCompilerArguments.append(" -context-root " + contextRoot);
		// }

		if (configFiles != null) {
			// NOTE: using just '=' causes internal build error with FlexBuilder
			// 3 on MacOSX with Eclipse Galileo.
			// String seperator = "=";
			String separator = "+=";
			for (File cfg : configFiles) {
				additionalCompilerArguments.append(" -load-config");
				additionalCompilerArguments.append(separator);
				additionalCompilerArguments.append(PathUtil.relativePath(project.getBasedir(), cfg));
				// separator = "+=";
			}
		}

		if (SWF.equals(packaging) || AIR.equals(packaging)) {
			File sourceFile = SourceFileResolver.resolveSourceFile(project.getCompileSourceRoots(), this.sourceFile, project.getGroupId(), project.getArtifactId());

			if (sourceFile == null) {
				throw new MojoExecutionException("Could not find main application! "
						+ "(Hint: Try to create a MXML file below your source root)");
			}

			String sourceRelativeToSourcePath = PathUtil.relativePath(sourceDirectory, sourceFile);

			context.put("mainApplication", sourceRelativeToSourcePath);
			getAllApplications().add(0, sourceRelativeToSourcePath);
			context.put("applications", getAllApplications());
			context.put("htmlGenerate", generateHtmlWrapper);
			context.put("cssfiles", buildCssFiles);
			context.put("autoExtract", autoExtractRSL);

			Iterator<Artifact> ait = project.getArtifacts().iterator();
			Artifact theme = null;
			while (ait.hasNext()) {
				Artifact artifact = ait.next();
				if (artifact.getScope() != null && artifact.getScope().equals("theme")) {
					theme = artifact;
					break;
				}
			}

			if (null != theme) {
				String themeLocation = null;
				if (theme.getArtifactId().equals("spark"))
					themeLocation = "${PROJECT_FRAMEWORKS}/themes/Spark";
				else if (theme.getArtifactId().equals("halo"))
					themeLocation = "${PROJECT_FRAMEWORKS}/themes/Halo";
				else if (theme.getArtifactId().equals("wireframe"))
					themeLocation = "${PROJECT_FRAMEWORKS}/themes/Wireframe";
				else if (theme.getArtifactId().equals("aeon-graphical"))
					themeLocation = "${PROJECT_FRAMEWORKS}/themes/AeonGraphical";
				else if (theme.getArtifactId().equals("mobile"))
					themeLocation = "${PROJECT_FRAMEWORKS}/themes/Mobile";

				context.put("themeLocation", themeLocation);
			}

		} else if (SWC.equals(packaging)) {
			context.put("mainApplication", project.getArtifactId() + ".as");
			context.put("htmlGenerate", false);

			// Warning: Tried to put in .flexConfig.xml but FlexBuilder
			// complains that it doesn't know what
			// "include-sources" is.
			if (includeClasses == null && includeSources == null && includeNamespaces == null) {
				// Changed to relative paths to eliminate issues with spaces.
				additionalCompilerArguments.append(" -include-sources ").append(plain(cleanSources(getSourceRoots())));
			} else if (includeSources != null) {
				additionalCompilerArguments.append(" -include-sources ").append(getPlainSources());
			}

			// Warning: Tried to add to .flexConfig.xml but didn't work... so
			// adding to compiler args.
			if (includeNamespaces != null && includeNamespaces.length > 0) {
				String namespaceStr = "";
				for (String namespace : includeNamespaces) {
					namespaceStr += namespace + " ";
				}
				additionalCompilerArguments.append(" -include-namespaces ").append(namespaceStr);
			}
		}
		context.put("additionalCompilerArguments", additionalCompilerArguments.toString());
		context.put("sources", getRelativeSources());
		context.put("PROJECT_FRAMEWORKS", "${PROJECT_FRAMEWORKS}"); // flexbuilder
																	// required
		context.put("libraryPathDefaultLinkType", getLibraryPathDefaultLinkType()); // change
																					// flex
																					// framework
																					// linkage
		context.put("pureActionscriptProject", pureActionScriptProject);
		context.put("moduleFiles", modules);

		return context;
	}

	protected VelocityContext getFlexPropertiesContext() {
		return new VelocityContext();
	}

	protected String getFlexPropertiesTemplate() {
		return "/templates/flexbuilder/flexProperties.vm";
	}

	private void writeFlexProperties() throws MojoExecutionException {
		runVelocity(getFlexPropertiesTemplate(), ".flexProperties", getFlexPropertiesContext());
	}

	protected VelocityContext getFlexLibPropertiesContext() {
		VelocityContext context = new VelocityContext();
		context.put("flexClasses", includeClasses);
		context.put("includeFiles", getResourceEntries(includeFiles));
		return context;
	}

	protected String getFlexLibPropertiesTemplate() {
		return "/templates/flexbuilder/flexLibProperties.vm";
	}

	private void writeFlexLibProperties() throws MojoExecutionException {
		runVelocity(getFlexLibPropertiesTemplate(), ".flexLibProperties", getFlexLibPropertiesContext());
	}

	/**
	 * Utility function to put html wrapper files in the location that flex
	 * builder expects them.
	 * 
	 * @throws MojoExecutionException
	 */
	private void writeHtmlTemplate() throws MojoExecutionException {
		if (generateHtmlWrapper) {
			// delete existing html template
			File outputDir = ideTemplateOutputFolder;
			if (outputDir.exists()) {
				outputDir.delete();
			}

			// HtmlWrapperUtil.extractTemplate(project, templateURI, outputDir);
		}
	}

	private Collection<String> getLocales() {
		Set<String> localesList = new HashSet<String>();
		if (localesCompiled != null) {
			localesList.addAll(Arrays.asList(localesCompiled));
		}
		if (localesRuntime != null) {
			localesList.addAll(Arrays.asList(localesRuntime));
		}
		if (localesList.isEmpty()) {
			localesList.add(defaultLocale);
		}
		return localesList;
	}

	/**
	 * Combines themes passed in on the themes property with themes that are
	 * added as Maven dependencies with scope theme
	 */
	private List<String> getThemes(Collection<FbIdeDependency> deps) {
		List<String> allThemes = new ArrayList<String>();

		Iterator<Artifact> ait = project.getArtifacts().iterator();

		Artifact theme = null;

		while (ait.hasNext()) {
			Artifact artifact = ait.next();
			if (artifact.getScope() != null && artifact.getScope().equals("theme")) {
				getLog().info("Find a theme artifact!" + artifact.getId());
				theme = artifact;
				break;
			}
		}

		if (theme != null) {
			Iterator<FbIdeDependency> it = deps.iterator();

			while (it.hasNext()) {
				FbIdeDependency dp = it.next();

				if (dp.getGroupId().equals(theme.getGroupId()) && dp.getArtifactId().equals(theme.getArtifactId())) {
					if (!dp.isReferencedProject()) {
						// get the absolute path

						ArtifactResolutionRequest request = new ArtifactResolutionRequest();
						request.setArtifact(theme);
						request.setLocalRepository(localRepository);
						request.setRemoteRepositories(remoteRepositories);
						ArtifactResolutionResult result = repositorySystem.resolve(request);
						if (result.isSuccess()) {
							allThemes.add(theme.getFile().getAbsolutePath());
						}

					} else {
						allThemes.add(".." + dp.getPath());
					}
				}
			}
		}

		Collections.reverse(allThemes);

		if (themes != null) {
			allThemes.addAll(Arrays.asList(themes));
		}

		return allThemes;
	}

	public Set<Artifact> getDependencies() {
		return Collections.unmodifiableSet(project.getArtifacts());
	}

	protected Set<Artifact> getDependencies(Matcher<? extends Artifact>... matchers) {
		Set<Artifact> dependencies = getDependencies();

		return new LinkedHashSet<Artifact>(filter(allOf(matchers), dependencies));
	}

	protected Artifact getDependency(Matcher<? extends Artifact>... matchers) {
		return selectFirst(getDependencies(), allOf(matchers));
	}

	protected void runVelocity(String templateName, String fileName, VelocityContext context)
			throws MojoExecutionException {

		Writer writer = null;
		try {
			Template template = velocityComponent.getEngine().getTemplate(templateName);
			writer = new FileWriter(new File(project.getBasedir(), fileName));
			template.merge(context, writer);
			writer.flush();
		} catch (Exception e) {
			throw new MojoExecutionException("Error writting " + fileName, e);
		} finally {
			if (writer != null) {
				IOUtil.close(writer);
			}
		}
	}

	protected Collection<FbIdeDependency> getExcludeSdkDependencies(Collection<FbIdeDependency> dependencies,
			List<LocalSdkEntry> excludes) {
		LinkedHashSet<FbIdeDependency> excludeDeps = new LinkedHashSet<FbIdeDependency>();
		Iterator<FbIdeDependency> iter = dependencies.iterator();
		while (iter.hasNext()) {
			FbIdeDependency dep = iter.next();
			if (excludes.contains(dep.getLocalSdkEntry())) {
				excludeDeps.add(dep);
			}
		}
		return excludeDeps;
	}

	protected Collection<FbIdeDependency> getModifiedSdkDependencies(Collection<FbIdeDependency> dependencies,
			List<LocalSdkEntry> modified) {
		LinkedHashSet<FbIdeDependency> modDeps = new LinkedHashSet<FbIdeDependency>();
		Iterator<FbIdeDependency> iter = dependencies.iterator();
		while (iter.hasNext()) {
			FbIdeDependency dep = iter.next();
			if (modified.contains(dep.getLocalSdkEntry())) {
				modDeps.add(dep);
			}
		}
		return modDeps;
	}

	protected Collection<FbIdeDependency> getNonSdkDependencies(Collection<FbIdeDependency> dependencies) {
		LinkedHashSet<FbIdeDependency> nonSdkDeps = new LinkedHashSet<FbIdeDependency>();
		Iterator<FbIdeDependency> iter = dependencies.iterator();
		while (iter.hasNext()) {
			FbIdeDependency dep = iter.next();
			if (!dep.getGroupId().equals("com.adobe.flex.framework"))
				nonSdkDeps.add(dep);
		}

		return nonSdkDeps;
	}

	private List<String> getAllApplications() {
		if (additionalApplications == null) {
			additionalApplications = new ArrayList<String>(10);
		}

		return additionalApplications;
	}

	private String getMainSources() {
		String mainSources = PathUtil.relativePath(project.getBasedir(), new File(project.getBuild().getSourceDirectory())).replace('\\', '/');
		return mainSources;
	}

	private String getPlainSources() {
		Collection<String> sources = new ArrayList<String>();
		File sourceDir = new File(project.getBuild().getSourceDirectory());
		for (File source : includeSources) {
			sources.add(PathUtil.relativePath(sourceDir, source));
		}
		return plain(sources);
	}

	String getPlainLocales() {
		Collection<String> locales = getLocales();
		String buf = plain(locales);
		return buf;
	}

	/**
	 * Looks for the Flex framework dependency and determines result depending
	 * on specified scope.
	 * 
	 * @return "1" if framework is merged into code, or "3" if its a runtime
	 *         shared library
	 * @throws MojoExecutionException
	 *             if framework dependency can not be found
	 */
	private String getLibraryPathDefaultLinkType() throws MojoExecutionException {
		LinkType type = sdk.getDefaultLinkType();

		return String.valueOf(type.getId());
	}

	/**
	 * Builds a collection of resource entries based of an array of files. Each
	 * ResourceEntry has a destination and a source. The source is the absolute
	 * path of the file include and the destination is a relative path starting
	 * at the source path root. The destination path is important because this
	 * is the path to where it will end up in the compiled SWC An example would
	 * be: Source path = ${basedir}/src/main/resources/org/proj/myfile.txt
	 * Destination path = org/proj/myfile.txt
	 * 
	 * @param includeFiles
	 * @return
	 */
	private Collection<ResourceEntry> getResourceEntries(File[] includeFiles) {
		Collection<ResourceEntry> entries = new ArrayList<ResourceEntry>();

		Collection<String> sourceRoots = getSourceRoots();

		if (includeFiles != null) {
			for (int i = 0; i < includeFiles.length; i++) {
				File includeFile = includeFiles[i];
				String sourcePath = includeFile.getAbsolutePath();

				// Strip source roots from destination and source paths.
				String destPath = "";
				for (String sourceRoot : sourceRoots) {
					if (sourcePath.contains(sourceRoot)) {
						int srl = sourceRoot.length();
						destPath = sourcePath.substring(srl + 1);
						sourcePath = destPath;
					}
				}

				// If the source path is not relative to any source roots
				// then the destination path will use the full source path.
				if (destPath.length() < 1) {
					destPath = sourcePath;
				}

				entries.add(new ResourceEntry(destPath, sourcePath));
			}
		}

		return entries;
	}

	/**
	 * Utility function to sense flex builder SDK value from framework
	 * dependencies. For example: 3.0.0 will import into Flexbuilder as "Flex 3"
	 * 3.2.0 will import as Flex "3.2".
	 * 
	 * @return
	 */
	protected String getFlexSdkVersion() {
		String value = "default";

		Artifact compiler = ArtifactsUtils.searchFor(pluginArtifacts, "com.adobe.flex", "compiler", null, "pom", null);
		if (compiler != null) {
			value = "Flex ";
			int[] version = splitVersion(compiler.getVersion()); // compiler.getVersion().split("\\.");
			for (int i = 0; i < 3; i++) {
				// take the first digit as it is
				if (i < 1) {
					value += Integer.toString(version[i]) + ".";
				}
				// if the last digit is not zero use it otherwise drop it.
				else if (version[i] != 0) {
					value += Integer.toString(version[i]) + ".";
				}
			}

			// remove the trailing . if it exists.
			if (value.endsWith("."))
				value = value.substring(0, value.length() - 1);
		}

		return value;

	}

	/**
	 * Utility function to rid sources of paths that include {locale} in them.
	 * These type of paths do NOT play well with flex builder -include-sources
	 * so best to just leave them out.
	 * 
	 * @param sources
	 * @return
	 */
	private Collection<String> cleanSources(Collection<String> sources) {
		String[] strings = sources.toArray(new String[0]);
		Collection<String> cleaned = new LinkedHashSet<String>();
		File sourceDir = new File(project.getBuild().getSourceDirectory());
		for (int i = 0; i < strings.length; i++) {
			if (!strings[i].contains("{locale}")) {
				// Convert to relative path to solve issues with spaces.
				String relativePath = (sourceDir.getAbsolutePath().equals(strings[i])) ? "."
						: PathUtil.relativePath(sourceDir, new File(strings[i]));
				cleaned.add(relativePath);
			}
		}

		return cleaned;
	}
}
