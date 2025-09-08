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
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static ch.ge.cti_composant.gitsync.util.MissionUtils.isUserCompliant;
import static org.gitlab4j.api.models.AccessLevel.MAINTAINER;

/**
 * Adds the authorized users to GitLab (BR2).
 */
public class AddAuthorizedUsersToGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(AddAuthorizedUsersToGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: adding the users to the authorized groups");
		GitlabAPIWrapper api = gitlab.getApi();



        // QC-514 : code bidon temp pour déclencher une faille Sonar java:S4426
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024); // Noncompliant
        } catch (NoSuchAlgorithmException e) {
            LOGGER.warn("error");
        }



        Map<String, User> allGitlabUsers = MissionUtils.getAllGitlabUsers(api);
		LOGGER.info("Total number of GitLab users: {}", allGitlabUsers.size());

		gitlab.getGroups()
				.forEach(group -> processGroup(group, ldapTree, api, allGitlabUsers));
		LOGGER.info("Mapping completed");
	}

	private void processGroup(Group group, LdapTree ldapTree, GitlabAPIWrapper api, Map<String, User> allGitlabUsers) {
		List<Member> memberList = api.getGroupMembers(group);
		LOGGER.info("    Processing the users of group [{}]", group.getName());

		Set<String> ldapUserNames = new TreeSet<>(ldapTree.getUsers(group.getName()).keySet());
		for (String ldapUserName : ldapUserNames) {
			User user = allGitlabUsers.get(ldapUserName);
			boolean isUserAlreadyMemberOfGroup = MissionUtils.isGitlabUserMemberOfGroup(memberList, ldapUserName);

			if (!allGitlabUsers.containsKey(ldapUserName)) {
				LOGGER.info("        User [{}] does not exist in GitLab (handling of group [{}])", ldapUserName, group.getName());
				continue;
			}

			if (isUserAlreadyMemberOfGroup) {
				handleExistingMember(memberList, ldapUserName, group, api, user);
			} else {
				handleNewMember(ldapUserName, group, api, user);
			}
		}
	}

	private void handleExistingMember(List<Member> memberList, String username, Group group, GitlabAPIWrapper api, User user) {
		if (MissionUtils.validateGitlabGroupMemberHasMinimumAccessLevel(memberList, username, MAINTAINER)) {
			LOGGER.info("        User [{}] is already in group [{}]", username, group.getName());
		} else {
			LOGGER.info("    Promoting user [{}] as maintainer to group {}", username, group.getName());
			api.deleteGroupMember(group, user.getId());
			api.addGroupMember(group, user.getId(), MAINTAINER);
		}
	}

	private void handleNewMember(String username, Group group, GitlabAPIWrapper api, User user) {
		if (isUserCompliant(username)) {
			LOGGER.info("        Adding user [{}] to group [{}]", username, group.getName());
			api.addGroupMember(group, user.getId(), MAINTAINER);
		} else {
			LOGGER.info("        Not adding user [{}] to group [{}], because it is banned", username, group.getName());
		}
	}
}
