/**
 * 
 */
package org.pureart.flexworks.plugin.ide.sdk;

import java.util.HashMap;

import org.pureart.flexworks.plugin.ide.ProjectType;

/**
 * @author pureart
 * 
 */
@SuppressWarnings("serial")
public class ProjectLinkTypeMap extends HashMap<ProjectType, ProjectLinkTypeEntry>
{

	/**
	 * Constructor
	 */
	public ProjectLinkTypeMap(ProjectLinkTypeEntry... entries)
	{
		super();
		for (ProjectLinkTypeEntry entry : entries)
		{
			this.put(entry.getProjectType(), entry);
		}
	}

	public boolean contains(ProjectType projectType)
	{
		return this.containsKey(projectType);
	}

}
