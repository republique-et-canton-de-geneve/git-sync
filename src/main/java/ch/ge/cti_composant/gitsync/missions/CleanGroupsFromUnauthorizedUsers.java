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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

import static org.apache.commons.lang3.StringUtils.toRootLowerCase;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;
import static org.gitlab4j.api.models.AccessLevel.MAINTAINER;

/**
 * Removes the permissions in excess on GitLab (BR2).
 * <br/>
 * Admin users are ignored. They can be assigned to any type of group or project.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanGroupsFromUnauthorizedUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: removing the user permissions in excess on GitLab");

		String ownerGroup = MissionUtils.getOwnerGroup();
		LOGGER.info("    Property owner-group is set to [{}]", ownerGroup);

		// Users in owner group
		final Map<String, LdapUser> owners = new HashMap<>();
		if (StringUtils.isNotBlank(ownerGroup) && ldapTree.getGroups().contains(new LdapGroup(ownerGroup))) {
			owners.putAll(ldapTree.getUsers(ownerGroup));
		}

		// for every group...
		gitlab.getGroups().stream()
				.sorted(Comparator.comparing(Group::getName))
				.forEach(gitlabGroup -> {
					LOGGER.info("    Processing group [{}]", gitlabGroup.getName());
					handleGroup(gitlabGroup, ldapTree, gitlab, owners);
				});

		LOGGER.info("Mapping completed");
	}

	private void handleGroup(Group gitlabGroup, LdapTree ldapTree, Gitlab gitlab, Map<String, LdapUser> owners) {
		LdapGroup ldapGroup = new LdapGroup(gitlabGroup.getName());
		GitlabAPIWrapper api = gitlab.getApi();
		List<Member> members = api.getGroupMembers(gitlabGroup);
		Map<String, User> gitlabUsers = MissionUtils.getAllGitlabUsers(api);

		members.stream()
				.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername())
						&& MissionUtils.isUserCompliant(member.getUsername()))  // will be handled below
				.filter(member -> !MissionUtils.getNotToCleanUsers().contains(member.getUsername()))
				.filter(member -> !owners.containsKey(member.getUsername()))
				.filter(member -> member.getAccessLevel() == MAINTAINER)
				.filter(member -> !member.getUsername().contains("_bot"))
				.forEach(member -> removeUser(member, gitlabGroup, api, ""));

		members.stream()
				.filter(member -> !MissionUtils.isUserCompliant(member.getUsername()))
				.forEach(member -> removeUser(member, gitlabGroup, api, " (banned user)"));

		members.stream()
				.filter(member -> MissionUtils.isGitlabUserExternal(gitlabUsers.get(member.getUsername())))
				.filter(member -> member.getAccessLevel() == MAINTAINER || member.getAccessLevel() == DEVELOPER)
				.forEach(member -> removeUser(member, gitlabGroup, api, " (external user)"));
	}

	private void removeUser(Member member, Group group, GitlabAPIWrapper api, String cause) {
		LOGGER.info("        Removing user [{}] ({}) from group [{}]{}",
				member.getUsername(), toRootLowerCase(member.getAccessLevel().name()), group.getName(), cause);
		api.deleteGroupMember(group, member.getId());
	}

}
