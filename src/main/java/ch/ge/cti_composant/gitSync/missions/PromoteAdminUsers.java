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
package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds the Admin users if they are present in the LDAP group.
 */
public class PromoteAdminUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PromoteAdminUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: adding admin users");
		GitlabAPIWrapper api = gitlab.getApi();

		Map<String, GitlabUser> allUsers = new HashMap<>();
		api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

		String adminGroup = MissionUtils.getAdministratorGroup();
		if (adminGroup == null) {
			LOGGER.info("    No administrator group defined");
		} else {
			ldapTree.getUsers(new LdapGroup(adminGroup))
					.forEach((username, ldapUser) -> {
						boolean userExists = MissionUtils.validateGitlabUserExistence(
								ldapUser, new ArrayList<>(allUsers.values()));
						if (userExists && !allUsers.get(username).isAdmin()) {
							LOGGER.info("    Setting user [{}] as administrator", username);
							api.updateUser(
									allUsers.get(username).getId(), allUsers.get(username).getEmail(), null,
									null, null, null, null, null, null,
									null, null, null, null, true, null);
						} else if (userExists &&
								MissionUtils.isGitlabUserAdmin(allUsers.get(username), gitlab.getApi(), ldapTree)) {
							LOGGER.info("    User [{}] is already administrator", username);
						} else {
							LOGGER.info("    User [{}] won't be set as administrator as it does not exist in GitLab",
									username);
						}
					});
		}

		LOGGER.info("Mapping completed");
	}

}
