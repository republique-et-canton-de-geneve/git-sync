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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

/**
 * Adds the owner users to all groups except if already admin (BR5).
 */
public class PropagateOwnerUsersToAllGroups implements Mission {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropagateOwnerUsersToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
	LOGGER.info("Propagating owner users to all groups");
	GitlabAPIWrapper api = gitlab.getApi();

	Map<String, GitlabUser> allUsers = new HashMap<>();
	api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

	String ownerGroup = MissionUtils.getOwnerGroup();
	LOGGER.info("    Property owner-group is set to [{}]", ownerGroup);

	if (StringUtils.isNotBlank(ownerGroup) && ldapTree.getGroups().contains(new LdapGroup(ownerGroup))) {
		// Users in owner group
		Map<String, LdapUser> owners = ldapTree.getUsers(ownerGroup);

		gitlab.getGroups().stream()
		    // no op for the black-listed groups
		    .filter(group -> !MissionUtils.getBlackListedGroups().contains(group.getName()))
		    .filter(group -> MissionUtils.validateGroupnameCompliantStandardGroups(group.getName()))
		    .forEach(group -> manageGroup(api, group, allUsers, owners));
	}

	LOGGER.info("Propagating owner users completed");
	}

	private void manageGroup(GitlabAPIWrapper api, GitlabGroup group, Map<String, GitlabUser> allUsers,
		Map<String, LdapUser> owners) {
		List<GitlabGroupMember> members = api.getGroupMembers(group);
		owners.forEach((username, ldapUser) -> setUserAsOwner(api, username, group, ldapUser, allUsers, members));
	}

	private void setUserAsOwner(GitlabAPIWrapper api, String username, GitlabGroup group, LdapUser ldapUser,
		Map<String, GitlabUser> allUsers, List<GitlabGroupMember> members) {
		boolean userExists = MissionUtils.validateGitlabUserExistence(ldapUser, new ArrayList<>(allUsers.values()));
		if (userExists) {
			// user is admin, do nothing
			if (allUsers.get(username).isAdmin()) {
				LOGGER.info("    User [{}] won't be set as owner to group {} as he is already admin in GitLab",
				            username, group.getName());
			}
			// user is not member, add it
			else if (!MissionUtils.isGitlabUserMemberOfGroup(members, username)) {
				LOGGER.info("    Setting user [{}] as owner to group {}", username, group.getName());
				api.addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Owner);
			}
			// user is member but not owner
			else if (!MissionUtils.validateGitlabGroupMemberHasMinimumAccessLevel(members, username,
			         GitlabAccessLevel.Owner)) {
				LOGGER.info("    Promoting user [{}] as owner to group {}", username, group.getName());
				api.deleteGroupMember(group, allUsers.get(username));
				api.addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Owner);
			}
			// user is already owner
			else {
				LOGGER.info("    User [{}] is already owner to group {}", username, group.getName());
			}
		}
		else {
			LOGGER.info("    User [{}] won't be set as owner to group {} as it does not exist in GitLab", username,
			            group.getName());
		}
	}

}
