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
import org.gitlab.api.models.GitlabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Removes the permissions in excess on GitLab.
 * <br/>
 * Admin users are ignored. They can be assigned to any type of group or project.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanGroupsFromUnauthorizedUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: removing the user permissions in excess on GitLab");

		// for every group...
		gitlab.getGroups().stream()
				.sorted(Comparator.comparing(GitlabGroup::getName))
				.forEach(gitlabGroup -> {
					LOGGER.info("    Processing group [{}]", gitlabGroup.getName());
					handleGroup(gitlabGroup, ldapTree, gitlab);
			});

		LOGGER.info("Mapping completed");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LdapTree ldapTree, Gitlab gitlab) {
		LdapGroup ldapGroup = new LdapGroup(gitlabGroup.getName());
		GitlabAPIWrapper api = gitlab.getApi();

		// for every user...
		api.getGroupMembers(gitlabGroup).stream()
				.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
				.filter(member -> !MissionUtils.isGitlabUserAdmin(member, api, ldapTree))
				.filter(member -> !MissionUtils.getWideAccessUsers().contains(member.getUsername()))
				.filter(member -> !MissionUtils.getNotToCleanUsers().contains(member.getUsername()))
				.forEach(member -> {
					LOGGER.info("        Removing user [{}] from group [{}]",
							member.getUsername(), gitlabGroup.getName());
					api.deleteGroupMember(gitlabGroup, member);
				});
	}

}
