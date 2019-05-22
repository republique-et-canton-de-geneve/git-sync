/*
 * gitSync
 *
 * Copyright (C) 2017-2019 République et canton de Genève
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ge.cti_composant.gitSync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to spawn a {@link GitSync}.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args){
		if (args.length != 1) {
			LOGGER.error("1 argument expected: the path to configuration file, such as configuration.properties");
		} else {
			new GitSync().run(args[0]);
			LOGGER.info("GitSync completed");
		}
	}

}
