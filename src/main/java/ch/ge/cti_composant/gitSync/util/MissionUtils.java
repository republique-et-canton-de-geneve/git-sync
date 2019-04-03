package ch.ge.cti_composant.gitSync.util;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import org.gitlab.api.GitlabAPI;
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
import java.util.stream.Collectors;

/**
 * Helper methods for the missions.
 */
public class MissionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(MissionUtils.class);

	private MissionUtils() {
	}

	/**
	 * Checks that we are the Owner of the specified GitLab group.
	 * By "we", we mean "the GitLab (technical) user associated with the token used for the connection to GitLab".
	 * <br/>
	 * Note: in GitLab a group can have only one owner.
	 */
	public static boolean validateGitlabGroupOwnership(GitlabGroup gitlabGroup, GitlabAPI gitlabAPI) {
		try {
			for (GitlabGroupMember owner : gitlabAPI.getGroupMembers(gitlabGroup).stream()
					.filter(gitlabGroupMember -> gitlabGroupMember.getAccessLevel() == GitlabAccessLevel.Owner)
					.collect(Collectors.toList()))
			{
			    if (gitlabAPI.getUser().getUsername().equals(owner.getUsername())) {
					return true;
			    }
			}
			return false;
		} catch (IOException e) {
			LOGGER.error("Impossible d'obtenir des informations sur le groupe [{}]", gitlabGroup.getName());
		}
		return false;
	}

	/**
	 * Checks that the specified GitLab group exists also in the LDAP tree.
	 */
	public static boolean validateLdapGroupExistence(GitlabGroup gitlabGroup, LdapTree ldapTree) {
		return ldapTree.getGroups().contains(new LdapGroup(gitlabGroup.getName()));
	}

	/**
	 * Checks the existence of a Gitlab group from the specified LDAP group.
	 */
	public static boolean validateGitlabGroupExistence(LdapGroup ldapGroup, GitlabAPI api) {
		try {
			api.getGroup(ldapGroup.getName());
			LOGGER.debug("LDAP group [{}] exists in GitLab", ldapGroup.getName());
			return true;
		} catch (IOException e) {
			LOGGER.debug("LDAP group [{}] does not exist in GitLab", ldapGroup.getName());
		}
		return false;
	}

	/**
	 * Checks that the specified user exists in GitLab.
	 */
	public static boolean validateGitlabUserExistence(LdapUser user, List<GitlabUser> users) {
		long usersCount = users.stream()
				.filter(gitlabUser -> gitlabUser.getUsername().equals(user.getName()))
				.count();
		switch ((int) usersCount) {
			case 1:
				return true;
			case 0:
				return false;
			default:
				throw new IllegalStateException("More than user with name [" + user.getName() + "] has been found");
		}
	}

	/**
	 * Checks whether the specified user has admin rights.
	 */
	public static boolean isGitlabUserAdmin(GitlabUser user, GitlabAPI api, LdapTree ldapTree) {
		try {
			// is it "me"?
			boolean isTechnicalAccount = user.getUsername().equals(api.getUser().getUsername());
			boolean isTrivialAdmin = user.isAdmin();
			// is it in the LDAP admin group?
			boolean isLdapAdmin = ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(user.getUsername());
			return isLdapAdmin || isTechnicalAccount || isTrivialAdmin;
		} catch (IOException e) {
			LOGGER.error("Exception caught while assessing the privileges of user [{}]", user.getUsername(), e);
		}
		return false;
	}

	public static Map<String, GitlabUser> getAllGitlabUsers(GitlabAPI api) {
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));
			return allUsers;
		} catch (IOException e) {
			LOGGER.error("Exception caught while retrieving all GitLab users", e);
		}
		return new HashMap<>();
	}

	public static boolean isGitlabUserMemberOfGroup(List<GitlabGroupMember> members, String user){
		return members.stream()
				.filter(member -> member.getUsername().equals(user))
				.count() == 1;
	}

	public static GitlabUser getGitlabUser(GitlabAPI api, String username) {
			return getAllGitlabUsers(api).get(username);
	}

}
