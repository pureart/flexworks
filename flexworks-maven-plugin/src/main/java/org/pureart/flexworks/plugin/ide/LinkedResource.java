package org.pureart.flexworks.plugin.ide;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author Jeremy
 * 
 */
public class LinkedResource extends org.apache.maven.plugin.eclipse.LinkedResource
{

	/**
	 * Constructor
	 */
	public LinkedResource()
	{
		super();
		System.out.println("User FlashBuilder LinkedResource Entry!");
	}

	public LinkedResource(Xpp3Dom node)
	{
		Xpp3Dom nameNode = node.getChild("name");

		if (nameNode == null) { throw new IllegalArgumentException("No name node."); }

		setName(nameNode.getValue());

		Xpp3Dom typeNode = node.getChild("type");

		if (typeNode == null) { throw new IllegalArgumentException("No type node."); }

		setType(typeNode.getValue());

		Xpp3Dom locationNode = node.getChild("location");

		// fixed the eclipse created linkedResource named locationURI
		if (locationNode == null)
			locationNode = node.getChild("locationURI");

		if (locationNode == null) { throw new IllegalArgumentException("No location node."); }

		setLocation(locationNode.getValue());
	}

}
