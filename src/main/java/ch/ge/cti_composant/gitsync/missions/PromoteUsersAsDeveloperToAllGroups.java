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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * Set users as developer to all groups.
 */
public class PromoteUsersAsDeveloperToAllGroups implements Mission {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromoteUsersAsDeveloperToAllGroups.class);

    @Override
    public void start(LdapTree ldapTree, Gitlab gitlab) {
	LOGGER.info("Promoting users as developer to all groups");
	GitlabAPIWrapper api = gitlab.getApi();

	// GitLab users
	Map<String, GitlabUser> gitlabUsers = new TreeMap<>();
	api.getUsers().forEach(gitlabUser -> gitlabUsers.put(gitlabUser.getUsername(), gitlabUser));

	// Ldap users
	Set<LdapUser> ldapUsers = new HashSet<>();
	for (LdapGroup group : ldapTree.getGroups()) {
	    ldapUsers.addAll(ldapTree.getUsers(group).values());
	}

	gitlab.getGroups().stream().filter(group -> !MissionUtils.getLimitedAccessGroups().contains(group.getName()))
		.filter(group -> MissionUtils.validateGroupnameCompliantStandardGroups(group.getName()))
		// for each gitlab group
		.forEach(group -> manageGroup(api, group, gitlabUsers, ldapUsers));

	LOGGER.info("Promoting users as developer completed");
    }

    private void manageGroup(GitlabAPIWrapper api, GitlabGroup group, Map<String, GitlabUser> gitlabUsers, Set<LdapUser> ldapUsers) {
	List<GitlabGroupMember> members = api.getGroupMembers(group);
	ldapUsers.stream()
		.filter(user -> gitlabUsers.containsKey(user.getName())) // Keep only ldap users existing in GitLab
		.sorted(Comparator.comparing(LdapUser::getName))
		.forEach(user -> promoteUserAsDeveloper(api, group, user, members, gitlabUsers));
    }

    private void promoteUserAsDeveloper(GitlabAPIWrapper api, GitlabGroup group, LdapUser ldapUser,
	    List<GitlabGroupMember> members, Map<String, GitlabUser> gitlabUsers) {
	GitlabUser user = gitlabUsers.get(ldapUser.getName());
	if (!MissionUtils.isGitlabUserMemberOfGroup(members, user.getUsername())) {
	    LOGGER.info("    User [{}] not member, adding as developer to group [{}]", user.getUsername(),
		    group.getName());
	    api.addGroupMember(group, user, GitlabAccessLevel.Developer);
	}
	else if (!MissionUtils.validateGitlabGroupMemberHasMinimumAccessLevel(members, user.getUsername(),
		GitlabAccessLevel.Developer)) {
	    LOGGER.info("    Promoting user [{}] as developer to group [{}]", user.getUsername(), group.getName());
	    api.deleteGroupMember(group, user);
	    api.addGroupMember(group, user, GitlabAccessLevel.Developer);
	}
	else {
	    LOGGER.debug("    User [{}] has already an access level up or equal to developer to group [{}]",
		    user.getUsername(), group.getName());
	}
    }

}
