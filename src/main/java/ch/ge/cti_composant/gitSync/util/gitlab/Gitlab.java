package ch.ge.cti_composant.gitSync.util.gitlab;

import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The GitLab context.
 * Consists mainly in the GitLab tree (groups and users), usually restricted to the elements that come from
 * the LDAP server.
 */
public class Gitlab {

	private static final Logger LOGGER = LoggerFactory.getLogger(Gitlab.class);

	private GitlabAPI api;

	private Map<GitlabGroup, Map<String, GitlabUser>> tree;

	public Gitlab(Map<GitlabGroup, Map<String, GitlabUser>> tree, String url, String apiKey){
		this.tree = Objects.requireNonNull(tree);
		this.api = GitlabAPI.connect(url, apiKey);
	}

	/**
	 * Returns the stub to the GitLab server.
	 * <br/>
	 * In most cases this method should not be called, since improved methods (whose names start with "api") have
	 * been created in this class. These methods wrap the call to "api" and handle the IOException.
	 */
	public GitlabAPI getApi() {
		return api;
	}

	/**
	 * Returns all GitLab groups, sorted by names.
	 */
	public List<GitlabGroup> getGroups() {
		return new ArrayList<>(tree.keySet()).stream()
				.sorted(Comparator.comparing(GitlabGroup::getName))
				.collect(Collectors.toList());
	}

	/**
	 * Returns all users of the specified GitLab group.
	 * @return a map where every key is a user name and every value is a GitLab user with that name
	 */
	public Map<String, GitlabUser> getUsers(GitlabGroup group) {
		return new HashMap<>(tree.getOrDefault(group, new HashMap<>()));
	}

	/**
	 * Returns all users.
	 * @return a map where every key is a user name and every value is a GitLab user with that name
	 */
	public Map<String, GitlabUser> getUsers() {
		HashMap<String, GitlabUser> output = new HashMap<>();
		tree.forEach((group, users) -> output.putAll(new HashMap<>(users)));
		return output;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getUser()}.
	 * Removes the checked exception.
	 */
	public GitlabUser apiGetUser() {
		GitlabUser ret;
		try {
			ret = getApi().getUser();
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#updateUser(Integer, String, String, String, String, String, String, String, String, Integer, String, String, String, Boolean, Boolean)} getUser()}.
	 * Removes the checked exception.
	 */
	public GitlabUser apiUpdateUser(
			Integer targetUserId,
			String email, String password, String username,
			String fullName, String skypeId, String linkedIn,
			String twitter, String website_url, Integer projects_limit,
			String extern_uid, String extern_provider_name,
			String bio, Boolean isAdmin, Boolean can_create_group) {
		GitlabUser ret;
		try {
			ret = getApi().updateUser(targetUserId,
					email, password, username,
					fullName, skypeId, linkedIn,
					twitter, website_url, projects_limit,
					extern_uid, extern_provider_name,
					bio, isAdmin, can_create_group);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getUsers()}.
	 * Removes the checked exception.
	 */
	public List<GitlabUser> apiGetUsers() {
		List<GitlabUser> ret;
		try {
			ret = getApi().getUsers();
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#createGroup(CreateGroupRequest, GitlabUser)}.
	 * Removes the checked exception.
	 */
	public GitlabGroup apiCreateGroup(CreateGroupRequest request, GitlabUser sudoUser) {
		GitlabGroup ret;
		try {
			ret = getApi().createGroup(request, sudoUser);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getGroupMembers(Integer)}.
	 * Removes the checked exception.
	 */
	public List<GitlabGroupMember> apiGetGroupMembers(Integer groupId) {
		List<GitlabGroupMember> ret;
		try {
			ret = getApi().getGroupMembers(groupId);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#addGroupMember(GitlabGroup, GitlabUser, GitlabAccessLevel)}.
	 * Removes the checked exception.
	 */
	public GitlabGroupMember apiAddGroupMember(GitlabGroup group, GitlabUser user, GitlabAccessLevel accessLevel) {
		GitlabGroupMember ret;
		try {
			ret = getApi().addGroupMember(group, user, accessLevel);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#deleteGroupMember(GitlabGroup, GitlabUser)}.
	 * Removes the checked exception.
	 */
	public void apiDeleteGroupMember(GitlabGroup group, GitlabUser user) {
		try {
			getApi().deleteGroupMember(group, user);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
	}


}
