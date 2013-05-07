package org.pureart.flexworks.plugin.ide;

import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWC;
import static org.sonatype.flexmojos.plugin.common.FlexExtension.SWF;

import org.sonatype.flexmojos.plugin.common.FlexExtension;

public enum ProjectType
{

	FLEX, FLEX_LIBRARY, ACTIONSCRIPT, AIR, AIR_LIBRARY;

	public static ProjectType getProjectType(String packaging, boolean useApoloConfig, boolean actionscript)
	{
		if (SWF.equals(packaging) && actionscript)
			return ACTIONSCRIPT;
		else if (FlexExtension.AIR.equals(packaging))
			return AIR;
		else if (SWF.equals(packaging) && !actionscript)
			return FLEX;
		else if (SWC.equals(packaging) && !useApoloConfig)
			return FLEX_LIBRARY;
		else if (SWC.equals(packaging) && useApoloConfig)
			return AIR_LIBRARY;
		return FLEX_LIBRARY;
	}

}
