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
package ch.ge.cti_composant.gitsync.service;

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupParams;
import org.gitlab4j.api.models.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for GitLab operations.
 */
public class GitlabService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabService.class);

	/**
	 * Constructs the in-memory GitLab tree (groups and users) from the specified LDAP tree.
	 *
	 * @return the GitLab tree, <b>restricted to the elements that come from the LDAP server</b>.
	 */
	public Gitlab buildGitlabContext(String hostname, String apiToken, LdapTree ldapTree) {
		// log on to GitLab
		LOGGER.info("Logging on to the GitLab server");
		GitlabAPIWrapper api = new GitlabAPIWrapper(new GitLabApi(hostname, apiToken));

		// create the missing groups on GitLab
		LOGGER.info("Creating the missing GitLab groups");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLdapGroupAdmin(ldapGroup))
				.filter(MissionUtils::validateGroupNameCompliantStandardGroups)
				.filter(ldapGroup -> !MissionUtils.validateGitlabGroupExistence(ldapGroup, api))
				.forEach(ldapGroup -> {
					LOGGER.info("    Group [{}] does not exist: creating it in GitLab", ldapGroup.getName());
					createGroup(ldapGroup, api);
				});

		// retrieve the GitLab groups
		LOGGER.info("Retrieving the GitLab groups");
		List<Group> groups = api.getGroups();

		// check and store the GitLab groups in memory
		LOGGER.info("Constructing la liste des groupes");

		Set<Group> groupsInLdap = groups.stream()
				// exclude the groups created independently of LDAP
				.filter(gitlabGroup -> MissionUtils.validateLdapGroupExistence(gitlabGroup, ldapTree))
				.collect(Collectors.toSet());

		return new Gitlab(groupsInLdap, hostname, apiToken);
	}

	private void createGroup(LdapGroup ldapGroup, GitlabAPIWrapper api) {
		GroupParams groupParams = new GroupParams()
				.withName(ldapGroup.getName())
				.withPath(ldapGroup.getName())
				.withVisibility(Visibility.INTERNAL.toValue());
		api.createGroup(groupParams);
	}

	private static boolean isLdapGroupAdmin(LdapGroup group) {
		return group.getName().equals(MissionUtils.getAdministratorGroup());
	}

}
