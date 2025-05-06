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

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;
import org.gitlab4j.api.models.AbstractUser;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

/**
 * Adds the Admin users if they are present in the LDAP group (BR4).
 */
public class PromoteAdminUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PromoteAdminUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: adding admin users");
		GitlabAPIWrapper api = gitlab.getApi();

		Map<String, User> allGitlabUsers = api.getUsers().stream()
				.collect(Collectors.toMap(AbstractUser::getUsername, user -> user, (v1, v2) -> v2));

		String adminGroup = MissionUtils.getAdministratorGroup();
		if (adminGroup == null) {
			LOGGER.info("    No administrator group defined");
		} else {
			ldapTree.getUsers(new LdapGroup(adminGroup))
					.forEach((username, ldapUser) ->
							handlePromotionToAdmin(ldapTree, username, ldapUser, allGitlabUsers, api));
		}
		LOGGER.info("Mapping completed");
	}

	private static void handlePromotionToAdmin(LdapTree ldapTree, String username, LdapUser ldapUser,
											   Map<String, User> allUsers, GitlabAPIWrapper api) {

		boolean userExists = MissionUtils.validateGitlabUserExistence(ldapUser, new ArrayList<>(allUsers.values()));

		if (!userExists) {
			LOGGER.info("    User [{}] won't be set as administrator as it does not exist in GitLab", username);
			return;
		}

		User user = allUsers.get(username);
		if (FALSE.equals(user.getIsAdmin())) {
			LOGGER.info("    Setting user [{}] as administrator", username);
			api.promoteToAdmin(user.getId());
		} else if (MissionUtils.isGitlabUserAdmin(user, api, ldapTree)) {
			LOGGER.info("    User [{}] is already administrator", username);
		}
	}

}
