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

import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adds the authorized users to GitLab.
 */
public class AddAuthorizedUsersToGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(AddAuthorizedUsersToGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: adding the users to the authorized groups");
		GitlabAPIWrapper api = gitlab.getApi();

		LOGGER.info("Total number of GitLab users: {}", api.getUsers().size());

		Map<String, GitlabUser> allUsers = new HashMap<>();
		api.getUsers()
				.forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

		for (GitlabGroup group : gitlab.getGroups()) {
			List<GitlabGroupMember> memberList = api.getGroupMembers(group);
			LOGGER.info("    Processing the users of group [{}]", group.getName());

			Set<String> userNames = new TreeSet<>(ldapTree.getUsers(group.getName()).keySet());
			for (String username : userNames) {
				boolean isUserAlreadyMemberOfGroup = memberList.stream()
						.filter(member -> member.getUsername().equals(username))
						.count() >= 1;

				if (allUsers.containsKey(username) && !isUserAlreadyMemberOfGroup) {
					// the user exists in GitLab and it has not been added to the group
					LOGGER.info("        Adding user [{}] to group [{}]", username, group.getName());
					try {
						api.addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Master);
					} catch (RuntimeException e) {
						// we'd rather not cancel the whole operation if an error occurs here
						LOGGER.warn("Error caught while adding user [{}] to group [{}]", username, group.getName(), e);
					}
				} else if (allUsers.containsKey(username) && isUserAlreadyMemberOfGroup) {
					// the user exists in GitLab and it has already been added to the group
					LOGGER.info("        User [{}] is already in group [{}]", username, group.getName());
				} else {
					// the user does not exist in GitLab
					LOGGER.info("        User [{}] does not exist in GitLab", username);
				}
			}
		}
		LOGGER.info("Mapping completed");
	}

}
