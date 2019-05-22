package ch.ge.cti_composant.gitSync.util;

import ch.ge.cti_composant.gitSync.GitSync;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
	 * Note: in GitLab a group can have only one Owner.
	 */
	public static boolean validateGitlabGroupOwnership(GitlabGroup gitlabGroup, GitlabAPIWrapper gitlabAPI) {
		for (GitlabGroupMember owner : gitlabAPI.getGroupMembers(gitlabGroup).stream()
				.filter(gitlabGroupMember -> gitlabGroupMember.getAccessLevel() == GitlabAccessLevel.Owner)
				.collect(Collectors.toList())) {
		    if (gitlabAPI.getUser().getUsername().equals(owner.getUsername())) {
				return true;
		    }
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
	 * Checks that the specified group exists in GitLab.
	 */
	public static boolean validateGitlabGroupExistence(LdapGroup ldapGroup, GitlabAPIWrapper api) {
		try {
			api.getGroup(ldapGroup.getName());
			LOGGER.debug("Group [{}] exists in GitLab", ldapGroup.getName());
			return true;
		} catch (GitSyncException e) {
			LOGGER.debug("Group [{}] does not exist in GitLab", ldapGroup.getName());
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
				throw new GitSyncException("More than one user with name [" + user.getName() + "] has been found");
		}
	}

	/**
	 * Checks whether the specified GitLab user has admin rights.
	 */
	public static boolean isGitlabUserAdmin(GitlabUser user, GitlabAPIWrapper api, LdapTree ldapTree) {
		// is it "me"?
		boolean isTechnicalAccount = user.getUsername().equals(api.getUser().getUsername());
		boolean isTrivialAdmin = user.isAdmin();
		// is it in the LDAP admin group?
		boolean isLdapAdmin = ldapTree.getUsers(getAdministratorGroup()).containsKey(user.getUsername());
		return isLdapAdmin || isTechnicalAccount || isTrivialAdmin;
	}

	public static Map<String, GitlabUser> getAllGitlabUsers(GitlabAPIWrapper api) {
		Map<String, GitlabUser> allUsers = new HashMap<>();
		api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));
		return allUsers;
	}

	public static boolean isGitlabUserMemberOfGroup(List<GitlabGroupMember> members, String user){
		return members.stream()
				.filter(member -> member.getUsername().equals(user))
				.count() == 1;
	}

	public static GitlabUser getGitlabUser(GitlabAPIWrapper api, String username) {
		return getAllGitlabUsers(api).get(username);
	}

	/**
	 * Gets the name of the LDAP group considered as the administrator group.
	 * See more about this in the README file and in the configuration file.
	 * @return a group name, or null if no administrator group is defined in the configuration file.
	 */
	public static String getAdministratorGroup() {
		String groupName = GitSync.getProperty("admin-group");
		return StringUtils.isBlank(groupName) ? null : groupName;
	}

	/**
	 * Gets the list of black listed groups from the configuration file.
	 * See more about this in the README file and in the configuration file.
	 * @return a list of user names. Can be empty
	 */
	public static List<String> getBlackListedGroups() {
		String groupNames = GitSync.getProperty("black-listed-groups");
		groupNames = StringUtils.isBlank(groupNames) ? "" : groupNames;
		return Stream.of(groupNames.split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

	/**
	 * Gets the list of the users to be assigned read-only access extensively on most groups.
	 * See more about this in the README file and in the configuration file.
	 * @return a list of GitLab user names. Can be empty
	 */
	public static List<String> getWideAccessUsers() {
		String userNames = GitSync.getProperty("wide-access-users");
		userNames = StringUtils.isBlank(userNames) ? "" : userNames;
		return Stream.of(userNames.split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

}
