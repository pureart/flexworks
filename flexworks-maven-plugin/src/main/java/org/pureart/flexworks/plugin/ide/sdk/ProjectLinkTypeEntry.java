/**
 * 
 */
package org.pureart.flexworks.plugin.ide.sdk;

import org.pureart.flexworks.plugin.ide.ProjectType;

/**
 * @author pureart
 * 
 */
public class ProjectLinkTypeEntry {

	private final ProjectType projectType;
	private final LinkType linkType;
	private final Integer index;

	/**
	 * @param projectType
	 * @param linkType
	 */
	public ProjectLinkTypeEntry(ProjectType projectType, LinkType linkType) {
		this(projectType, linkType, null);
	}

	/**
	 * @param projectType
	 * @param linkType
	 * @param index
	 */
	public ProjectLinkTypeEntry(ProjectType projectType, LinkType linkType,
			Integer index) {
		super();
		this.projectType = projectType;
		this.linkType = linkType;
		this.index = index;
	}

	public ProjectType getProjectType() {
		return projectType;
	}

	public LinkType getLinkType() {
		return linkType;
	}

	public Integer getIndex() {
		return index;
	}

}
