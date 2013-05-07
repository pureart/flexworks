/**
 * 
 */
package org.pureart.flexworks.plugin.ide.sdk;

import java.io.File;
import java.util.regex.Pattern;
import org.pureart.flexworks.plugin.ide.FbIdeDependency;
import org.pureart.flexworks.plugin.ide.ProjectType;
import org.sonatype.flexmojos.plugin.common.FlexScopes;

/**
 * @author pureart
 * 
 */
public class LocalSdkEntry implements Comparable<LocalSdkEntry> {

	private final String groupId;
	private final String artifactId;
	private final String path;
	private final String sourcePath;
	private final ProjectLinkTypeMap projectLinkTypeMap;
	private final ProjectType projectType;
	private LocalSdk sdk;

	/**
	 * @param sdk
	 * @param groupId
	 * @param artifactId
	 * @param path
	 * @param sourcePath
	 * @param projectType
	 * @param projectLinkTypeMap
	 */
	public LocalSdkEntry(LocalSdk sdk, String groupId, String artifactId, String path, String sourcePath,
			ProjectType projectType, ProjectLinkTypeMap projectLinkTypeMap) {
		super();
		this.sdk = sdk;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.path = path;
		this.sourcePath = sourcePath;
		this.projectLinkTypeMap = projectLinkTypeMap;
		this.projectType = projectType;
	}

	public Integer getIndex() {
		if (projectLinkTypeMap == null)
			return null;
		else
			return projectLinkTypeMap.get(projectType).getIndex();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LocalSdkEntry o) {
		Integer myDefaultIndex = this.getIndex();
		Integer theirDefaultIndex = o.getIndex();

		if (myDefaultIndex == null && theirDefaultIndex == null)
			return 0;
		else if (myDefaultIndex == null && theirDefaultIndex != null)
			return 1;
		else if (myDefaultIndex != null && theirDefaultIndex == null)
			return -1;
		else
			return myDefaultIndex - theirDefaultIndex;
	}

	/**
	 * Determines if this artifact is included in the current SDK version and
	 * project type where the project type is one of (Flex, Flex Library, AIR,
	 * AIR Library or ActionScript).
	 * 
	 * @return
	 */
	public boolean isProjectType() {
		boolean value = false;
		if (projectLinkTypeMap != null && projectLinkTypeMap.contains(projectType))
			value = true;

		return value;
	}

	/**
	 * Checks for differences between the IDE dependency and the local Flex SDK
	 * entry.
	 * 
	 * @param dependency
	 * @return
	 */
	public boolean isModified(FbIdeDependency dependency) {
		if (isLinkTypeModified(dependency))
			return true;

		return false;
	}

	private LinkType getLinkTypeFromIdeDependency(FbIdeDependency dependency) {
		LinkType depLinkType = LinkType.MERGE;
		if (FlexScopes.CACHING.equals(dependency.getScope())) {
			depLinkType = LinkType.RSL_DIGEST;
		} else if (FlexScopes.RSL.equals(dependency.getScope())) {
			depLinkType = LinkType.RSL;
		} else if (FlexScopes.MERGED.equals(dependency.getScope())) {
			depLinkType = LinkType.MERGE;
		} else if (FlexScopes.EXTERNAL.equals(dependency.getScope()) || "runtime".equals(dependency.getScope())) {
			depLinkType = LinkType.EXTERNAL;
		}
		// NOTE flex/flash builder doesn't support this link type so just merge.
		else if (FlexScopes.INTERNAL.equals(dependency.getScope())) {
			depLinkType = LinkType.MERGE;
		}

		return depLinkType;
	}

	private boolean isLinkTypeModified(FbIdeDependency dependency) {
		LinkType depLinkType = getLinkTypeFromIdeDependency(dependency);

		return (depLinkType != this.getLinkType());
	}

	/**
	 * Returns the link type of this artifact for the current SDK version and
	 * Project Type.
	 * 
	 * @return
	 */
	public LinkType getLinkType() {
		if (projectLinkTypeMap == null || projectType == null) {
			return LinkType.MERGE;
		} else {
			LinkType linkType = projectLinkTypeMap.get(projectType).getLinkType();
			if (linkType != null) {
				return linkType;
			} else {
				return LinkType.MERGE;
			}
		}
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getPath() {
		final File file = sdk.getFrameworkPath();
		if (null != file) {
			final String s = Pattern.quote("${PROJECT_FRAMEWORKS}");
			return path.replaceFirst(s, file.getPath());
		}
		return path;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public ProjectLinkTypeMap getProjectLinkTypeMap() {
		return projectLinkTypeMap;
	}

	public ProjectType getProjectType() {
		return projectType;
	}

}
