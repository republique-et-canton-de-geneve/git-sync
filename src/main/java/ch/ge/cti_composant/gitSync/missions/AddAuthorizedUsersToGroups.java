package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers()
					.forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			for (GitlabGroup group : gitlab.getGroups()) {
				List<GitlabGroupMember> memberList = gitlab.getApi().getGroupMembers(group.getId());
				LOGGER.info("Processing of the users of group [{}]", group.getName());

				Set<String> userNames = new TreeSet<>(ldapTree.getUsers(group.getName()).keySet());
				for (String username : userNames) {
					boolean isUserAlreadyMemberOfGroup = memberList.stream()
							.filter(member -> member.getUsername().equals(username))
							.count() == 1;

					if (allUsers.containsKey(username) && !isUserAlreadyMemberOfGroup) {
						// the user exists in GitLab and it has not been added to the group
						LOGGER.info("    Adding user [{}] to group [{}]", username, group.getName());
						gitlab.getApi().addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Master);
					} else if (allUsers.containsKey(username) && isUserAlreadyMemberOfGroup) {
						// the user exists in GitLab and it has already been added to the group
						LOGGER.info("    User [{}] is already in group [{}]", username, group.getName());
					} else {
						// the user does not exist in GitLab
						LOGGER.info("    User [{}] does not exist in GitLab", username);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Exception caught while retrieving the list of users", e);
		}
		LOGGER.info("Mapping completed");
	}

}
