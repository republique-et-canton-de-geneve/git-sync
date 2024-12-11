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
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds the admin users to all groups (BR4).
 */
public class PropagateAdminUsersToAllGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropagateAdminUsersToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Propagating admin users to all groups");
		GitlabAPIWrapper api = gitlab.getApi();

		List<GitlabUser> admins = api.getUsers().stream()
				.filter(GitlabUser::isAdmin)
				.filter(admin -> ldapTree.getUsers(MissionUtils.getAdministratorGroup()).containsKey(admin.getUsername()))
				.sorted(Comparator.comparing(GitlabUser::getUsername))
				.collect(Collectors.toList());
		gitlab.getGroups().stream()
				// no op for the black-listed groups
				.filter(group -> !MissionUtils.getBlackListedGroups().contains(group.getName()))
				.forEach(group -> {
						List<GitlabGroupMember> members = api.getGroupMembers(group);
						admins.stream()
								.forEach(admin -> {
									if (!MissionUtils.isGitlabUserMemberOfGroup(members, admin.getUsername())) {
										LOGGER.info("    Adding user [{}] to group [{}]",
												admin.getUsername(), group.getName());
										api.addGroupMember(group, admin, GitlabAccessLevel.Master);
									} else {
										LOGGER.debug("    User [{}] is already a member of group [{}]" ,
												admin.getUsername(), group.getName());
									}
								});
				});

		LOGGER.info("Propagating completed");
	}

}
