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
package org.pureart.flexworks.plugin.test;

import java.lang.reflect.Field;
import org.sonatype.flexmojos.plugin.test.TestRunMojo;

/**
 * 
 * @author pureart.org
 * @goal testRun
 * @phase test
 * @requiresDependencyResolution test
 * @extendsPlugin flexmojos
 * @extendsGoal test-run
 * @threadSafe
 */
public class FreeTestRunMojo extends TestRunMojo {

	public String getTargetPlayer() {
		try {
			Field f = super.getClass().getField("targetPlayer");
			if (null != f) {
				f.setAccessible(true);
				return (String) f.get(this);
			}
			return null;
		} catch (final Exception e) {

		}
		return null;
	}

	public void setTargetPlayer(String value) {
		try {
			Field f = super.getClass().getField("targetPlayer");
			if (null != f) {
				f.setAccessible(true);
				f.set(this, value);
			}
		} catch (final Exception e) {
		}
	}
}
