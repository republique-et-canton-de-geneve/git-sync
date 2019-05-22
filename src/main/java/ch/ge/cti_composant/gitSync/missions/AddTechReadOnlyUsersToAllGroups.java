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
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Grants read-only access to the wide-access users.
 */
public class AddTechReadOnlyUsersToAllGroups implements Mission {
    	
	private static final Logger LOGGER = LoggerFactory.getLogger(AddTechReadOnlyUsersToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Adding read-only accesses");

		MissionUtils.getWideAccessUsers()
				.forEach(username -> addUser(gitlab, username));

		LOGGER.info("Adding read-only accesses completed");
	}

	private void addUser(Gitlab gitlab, String username) {
		LOGGER.info("    Adding read-only access for user [{}] to all groups, with Reporter permissions", username);
		GitlabAPIWrapper api = gitlab.getApi();
		GitlabUser user = MissionUtils.getGitlabUser(api, username);
		if (user != null) {
			gitlab.getGroups().stream()
					// no op for the black-listed groups
					.filter(gitlabGroup -> !MissionUtils.getBlackListedGroups().contains(gitlabGroup.getName()))
					.forEach(gitlabGroup -> {
							List<GitlabGroupMember> members = api.getGroupMembers(gitlabGroup);
							if (!MissionUtils.isGitlabUserMemberOfGroup(members, username)) {
								LOGGER.info("        Adding user [{}] to group [{}]",
										username, gitlabGroup.getName());
								api.addGroupMember(gitlabGroup, user, GitlabAccessLevel.Reporter);
							} else {
								LOGGER.debug("        User [{}] is already a member of group [{}]",
										username, gitlabGroup.getName());
							}
					});
		} else {
			LOGGER.warn("        WARNING: user [{}] not found in GitLab, so it cannot be granted read-only access to groups",
					username);
		}
		LOGGER.info("    Adding read-only access for user [{}] to all groups completed", username);
	}

}
