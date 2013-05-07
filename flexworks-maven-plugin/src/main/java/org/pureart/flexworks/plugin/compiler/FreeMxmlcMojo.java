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
package org.pureart.flexworks.plugin.compiler;

import org.sonatype.flexmojos.plugin.compiler.MxmlcMojo;

/**
 *
 * @author pureart.org
 * 
 * @phase compile
 * @goal compileSwf
 * @extendsGoal compile-swf
 * @extendsPlugin flexmojos
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class FreeMxmlcMojo extends MxmlcMojo
{
	// just make lifecycle mapping here.
}
