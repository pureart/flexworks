/**
 * 
 */
package org.pureart.flexworks.plugin.ide.sdk;

/**
 * @author pureart
 * 
 */
public enum LinkType
{

	DEFAULT(0), MERGE(1), EXTERNAL(2), RSL(3), RSL_DIGEST(4);

	private int	id;

	private LinkType(int id)
	{
		this.id = id;
	}

	public int getId()
	{
		return this.id;
	}

}
