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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;

/**
 * Set users as developer to all groups (BR3)
 */
public class PromoteUsersAsDeveloperToAllGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PromoteUsersAsDeveloperToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Promoting users as developer to all groups");
		GitlabAPIWrapper api = gitlab.getApi();

		// GitLab users
		Map<String, User> gitlabUsers = new TreeMap<>();
		api.getUsers().forEach(gitlabUser -> gitlabUsers.put(gitlabUser.getUsername(), gitlabUser));

		// Ldap users
		Set<LdapUser> ldapUsers = MissionUtils.getLdapUsers(ldapTree);

		// Keep only compliant LDAP users existing in GitLab
		List<LdapUser> filteredUsers = ldapUsers.stream()
				.filter(user -> gitlabUsers.containsKey(user.getName()))
				.filter(user -> MissionUtils.isUserCompliant(user.getName()))
				.sorted(Comparator.comparing(LdapUser::getName)).toList();

		gitlab.getGroups().stream()
				.filter(group -> !MissionUtils.getLimitedAccessGroups().contains(group.getName()))
				.filter(group -> MissionUtils.validateGroupNameCompliantStandardGroups(group.getName()))
				// for each gitlab group
				.forEach(group -> {
					LOGGER.info("Promoting users as developer to group [{}]", group.getName());
					manageGroup(api, group, gitlabUsers, filteredUsers);
				});

		LOGGER.info("Promoting users as developer completed");
	}

	private void manageGroup(GitlabAPIWrapper api, Group group, Map<String, User> gitlabUsers, List<LdapUser> ldapUsers) {
		List<Member> members = api.getGroupMembers(group);
		ldapUsers.forEach(user -> promoteUserAsDeveloper(api, group, members, gitlabUsers.get(user.getName())));
	}

	private void promoteUserAsDeveloper(GitlabAPIWrapper api, Group group, List<Member> members, User user) {
		if (!MissionUtils.isGitlabUserMemberOfGroup(members, user.getUsername())) {
			LOGGER.info("    User [{}] not member, adding as developer to group [{}]", user.getUsername(), group.getName());
			api.addGroupMember(group, user.getId(), DEVELOPER);
		} else if (!MissionUtils.validateGitlabGroupMemberHasMinimumAccessLevel(members, user.getUsername(), DEVELOPER)) {
			LOGGER.info("    Promoting user [{}] as developer to group [{}]", user.getUsername(), group.getName());
			api.deleteGroupMember(group, user.getId());
			api.addGroupMember(group, user.getId(), DEVELOPER);
		} else {
			LOGGER.debug("    User [{}] has already an access level up or equal to developer to group [{}]",
					user.getUsername(), group.getName());
		}
	}

}
