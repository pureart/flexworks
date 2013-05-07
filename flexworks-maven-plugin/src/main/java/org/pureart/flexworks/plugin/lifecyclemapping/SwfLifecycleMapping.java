/*
 * PureArt Archetype. Make any work easier.
 *
 * Copyright (C) 2011  pureart.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.pureart.flexworks.plugin.lifecyclemapping;

import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.flexmojos.plugin.common.FlexExtension;

/**
 *
 * @author pureart.org
 */
@Component(role = LifecycleMapping.class, hint = FlexExtension.SWF)
public class SwfLifecycleMapping extends AbstractActionScriptLifecycleMapping implements LifecycleMapping
{

	@Override
	public String getCompiler()
	{
		return "org.pureart.maven.plugins:flexworks-maven-plugin:compileSwf";
	}
}
