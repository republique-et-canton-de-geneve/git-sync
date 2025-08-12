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

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.User;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gitlab4j.api.models.AccessLevel.ADMIN;

/**
 * Helper methods for the missions.
 */
public class MissionUtils {

	private static Pattern standardGroupsPattern;

	private static Pattern standardGroupUsersPattern;

	private MissionUtils() {
	}

	/**
	 * Checks that the specified GitLab group exists also in the LDAP tree.
	 */
	public static boolean validateLdapGroupExistence(Group gitlabGroup, LdapTree ldapTree) {
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
	public static boolean validateGroupNameCompliantStandardGroups(LdapGroup ldapGroup) {
		return validateGroupNameCompliantStandardGroups(ldapGroup.getName());
	}

	/**
	 * Checks that the specified ldap group name is compliant with standard groups regex.
	 */
	public static boolean validateGroupNameCompliantStandardGroups(String ldapGroup) {
		return getStandardGroupsPattern().matcher(ldapGroup).lookingAt();
	}

	/**
	 * Checks that the specified user name complies with the regex given in parameter "standard-group-users".
	 */
	public static boolean isUserCompliant(String username) {
		return getStandardGroupUsersPattern().matcher(username).lookingAt();
	}

	/**
	 * Checks that the specified user exists in GitLab.
	 */
	public static boolean validateGitlabUserExistence(LdapUser user, List<User> users) {
		long usersCount = users.stream()
				.filter(gitlabUser -> Objects.equals(gitlabUser.getUsername(), user.getName()))
				.count();

		if (usersCount > 1) {
			throw new GitSyncException("More than one user with name [" + user.getName() + "] has been found");
		}

		return usersCount == 1;
	}

	/**
	 * Checks whether the specified GitLab user has admin rights.
	 */
	public static boolean isGitlabUserAdmin(User user, GitlabAPIWrapper api, LdapTree ldapTree) {
		return Boolean.TRUE.equals(user.getIsAdmin()) || isIsLdapAdmin(user, ldapTree) || isTechnicalAccount(user, api);
	}

	/**
	 * Gets all the gitlab users and returns them in a map.
	 */
	public static Map<String, User> getAllGitlabUsers(GitlabAPIWrapper api) {
		return api.getUsers().stream().collect(
				Collectors.toMap(User::getUsername, user -> user, (v1, v2) -> v2));
	}

	/**
	 * Checks whether the specified GitLab user is in the members list.
	 */
	public static boolean isGitlabUserMemberOfGroup(List<Member> members, String username) {
		return members.stream()
				.anyMatch(member -> Objects.equals(member.getUsername(), username));
	}

	/**
	 * Checks whether the specified GitLab user has at least the specified access level.
	 */
	public static boolean validateGitlabGroupMemberHasMinimumAccessLevel(List<Member> members, String user, AccessLevel accesslevel) {
		return members.stream()
				.filter(member -> Objects.equals(member.getUsername(), user))
				.anyMatch(member -> member.getAccessLevel() == ADMIN || member.getAccessLevel().value >= accesslevel.value);
	}

	/**
	 * Gets the name of the LDAP group considered as the administrator group.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a group name, or null if no administrator group is defined in the configuration file.
	 */
	public static String getAdministratorGroup() {
		String groupName = GitSync.getProperty("admin-group");
		return StringUtils.isBlank(groupName) ? null : groupName;
	}

	/**
	 * Gets the name of the LDAP group considered as the owner group.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a group name, or null if no owner group is defined in the configuration file.
	 */
	public static String getOwnerGroup() {
		String groupName = GitSync.getProperty("owner-group");
		return StringUtils.isBlank(groupName) ? null : groupName;
	}

	/**
	 * Gets the list of black listed groups from the configuration file.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a list of user names. Can be empty
	 */
	public static List<String> getBlackListedGroups() {
		String groupNames = GitSync.getProperty("black-listed-groups");
		groupNames = StringUtils.isBlank(groupNames) ? "" : groupNames;
		return Stream.of(groupNames.split(","))
				.filter(StringUtils::isNotBlank)
				.toList();
	}

	/**
	 * Gets the list of limited access groups from the configuration file.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a list of user names. Can be empty
	 */
	public static List<String> getLimitedAccessGroups() {
		String groupNames = GitSync.getProperty("limited-access-groups");
		groupNames = StringUtils.isBlank(groupNames) ? "" : groupNames;
		return Stream.of(groupNames.split(","))
				.filter(StringUtils::isNotBlank)
				.toList();
	}

	/**
	 * Gets the list of the users to ignore in the cleaning process.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a list of GitLab user names. Can be empty
	 */
	public static List<String> getNotToCleanUsers() {
		String userNames = GitSync.getProperty("not-to-clean-users");
		userNames = StringUtils.isBlank(userNames) ? "" : userNames;
		return Stream.of(userNames.split(","))
				.filter(StringUtils::isNotBlank)
				.toList();
	}

	/**
	 * Gets the list of wide access listed users from the configuration file.
	 * See more about this in the README file and in the configuration file.
	 *
	 * @return a list of user names. Can be empty
	 */
	public static List<String> getWideAccessUsers() {
		String userNames = GitSync.getProperty("wide-access-users");
		userNames = StringUtils.isBlank(userNames) ? "" : userNames;
		return Stream.of(userNames.split(","))
				.filter(StringUtils::isNotBlank)
				.toList();
	}

	public static Set<LdapUser> getLdapUsers(LdapTree ldapTree) {
		Set<LdapUser> ldapUsers = new HashSet<>();
		for (LdapGroup group : ldapTree.getGroups()) {
			ldapUsers.addAll(ldapTree.getUsers(group).values());
		}
		return ldapUsers;
	}

	private static boolean isTechnicalAccount(User user, GitlabAPIWrapper api) {
		return user.getUsername().equals(api.getUser().getUsername());
	}

	private static boolean isIsLdapAdmin(User user, LdapTree ldapTree) {
		return ldapTree.getUsers(getAdministratorGroup()).containsKey(user.getUsername());
	}

	private static Pattern getStandardGroupsPattern() {
		if (standardGroupsPattern == null) {
			String patternString = GitSync.getProperty("standard.groups");
			if (StringUtils.isBlank(patternString)) {
				patternString = "[A-Za-z0-9_-]";
			}
			standardGroupsPattern = Pattern.compile(patternString);
		}
		return standardGroupsPattern;
	}

	private static Pattern getStandardGroupUsersPattern() {
		if (standardGroupUsersPattern == null) {
			String patternString = GitSync.getProperty("standard-group-users");
			if (StringUtils.isBlank(patternString)) {
				patternString = "[A-Za-z0-9_-]";
			}
			standardGroupUsersPattern = Pattern.compile(patternString);
		}
		return standardGroupUsersPattern;
	}

}
