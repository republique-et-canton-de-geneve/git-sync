/*
 * gitsync
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
package ch.ge.cti_composant.gitsync.missions;

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CheckMinimumUserCount implements Mission {

	/**
	 * Default value of property "minimum-user-count".
	 * It is used if no value for "minimum-user-count" is supplied in the configuration file.
	 */
	private static final int DEFAULT_MINIMUM_USER_COUNT = 5;

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckMinimumUserCount.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Precondition: check that the LDAP tree is not empty or almost empty");

		// A hashset is used to ensure a user is not added more than once
		Set<LdapUser> users = new HashSet<>();
		for (LdapGroup group : ldapTree.getGroups()) {
			users.addAll(ldapTree.getUsers(group).values());
		}

		// Make sure the minimal count of users has been found
		LOGGER.info("Total number of groups = {}", ldapTree.getGroups().size());
		int minimumUserCount = getMinimumUserCount();
		if (users.size() >= minimumUserCount) {
			LOGGER.info("Total number of users = {}", users.size());
		} else {
			String message = "Total number of users (" + users.size() + ") < minimum number of users ("
					+ minimumUserCount + ") -> we suspect a problem -> process aborted";
			LOGGER.error(message);
			throw new GitSyncException(message);
		}

		LOGGER.info("Precondition OK");
	}

	private int getMinimumUserCount() {
		String prop = GitSync.getProperty("minimum-user-count");
		return prop == null ? DEFAULT_MINIMUM_USER_COUNT : new Integer(prop);
	}

}
