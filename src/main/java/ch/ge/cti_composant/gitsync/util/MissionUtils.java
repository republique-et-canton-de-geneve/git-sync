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
package ch.ge.cti_composant.gitsync.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

/**
 * Helper methods for the missions.
 */
public class MissionUtils {

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
		return api.getGroup(ldapGroup.getName()) != null;
	}

	/**
	 * Checks that the specified ldap group is compliant with standard groups regex.
	 */
	public static boolean validateGroupnameCompliantStandardGroups(LdapGroup ldapGroup) {
	    return validateGroupnameCompliantStandardGroups(ldapGroup.getName());
	}

	/**
	 * Checks that the specified ldap group name is compliant with standard groups regex.
	 */
	public static boolean validateGroupnameCompliantStandardGroups(String ldapGroup) {
	    String patternString = GitSync.getProperty("standard.groups");
	    if(StringUtils.isBlank(patternString)) {
		patternString = "[A-Za-z0-9_-]";
	    }

	    Pattern pattern = Pattern.compile(patternString);
	    Matcher matcher = pattern.matcher(ldapGroup);

	    return matcher.lookingAt();
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

	public static boolean validateGitlabGroupMemberHasMinimumAccessLevel(List<GitlabGroupMember> members, String user, GitlabAccessLevel accesslevel) {
		return members.stream()
				.filter(member -> member.getUsername().equals(user))
				.filter(member -> member.isAdmin() || member.getAccessLevel().accessValue >= accesslevel.accessValue)
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
	 * Gets the name of the LDAP group considered as the owner group.
	 * See more about this in the README file and in the configuration file.
	 * @return a group name, or null if no owner group is defined in the configuration file.
	 */
	public static String getOwnerGroup() {
		String groupName = GitSync.getProperty("owner-group");
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
	 * Gets the list of limited access groups from the configuration file.
	 * See more about this in the README file and in the configuration file.
	 * @return a list of user names. Can be empty
	 */
	public static List<String> getLimitedAccessGroups() {
		String groupNames = GitSync.getProperty("limited-access-groups");
		groupNames = StringUtils.isBlank(groupNames) ? "" : groupNames;
		return Stream.of(groupNames.split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

	/**
	 * Gets the list of the users to ignore in the cleaning process.
	 * See more about this in the README file and in the configuration file.
	 * @return a list of GitLab user names. Can be empty
	 */
	public static List<String> getNotToCleanUsers() {
		String userNames = GitSync.getProperty("not-to-clean-users");
		userNames = StringUtils.isBlank(userNames) ? "" : userNames;
		return Stream.of(userNames.split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toList());
	}

}
