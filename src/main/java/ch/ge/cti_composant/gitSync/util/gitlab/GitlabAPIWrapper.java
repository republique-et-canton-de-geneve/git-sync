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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * A simple wrapper around a {@link GitlabAPI} object, to replace the checked exceptions with unchecked exceptions.
 * Not all methods of class GitLabAPI are overridden - only those relevant to this application.
 */
public class GitlabAPIWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabAPIWrapper.class);

	/**
	 * The wrapped GitlabAPI object.
	 */
	private GitlabAPI api;

	/**
	 * Constructor.
	 */
	public GitlabAPIWrapper(GitlabAPI api) {
		this.api = api;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getGroups()} .
	 * Removes the checked exception.
	 */
	public List<GitlabGroup> getGroups() {
		List<GitlabGroup> ret;
		try {
			ret = api.getGroups();
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getGroup(String)} .
	 * Removes the checked exception.
	 */
	public GitlabGroup getGroup(String path) {
		GitlabGroup ret = null;
		try {
			ret = api.getGroup(path);
		} catch (FileNotFoundException e) {
			// occurs when the group does not exist in GitLab
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
	public GitlabGroup createGroup(CreateGroupRequest request, GitlabUser sudoUser) {
		GitlabGroup ret;
		try {
			ret = api.createGroup(request, sudoUser);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getGroupMembers(GitlabGroup)}.
	 * Removes the checked exception.
	 */
	public List<GitlabGroupMember> getGroupMembers(GitlabGroup group) {
		List<GitlabGroupMember> ret;
		try {
			ret = api.getGroupMembers(group);
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
	public GitlabGroupMember addGroupMember(GitlabGroup group, GitlabUser user, GitlabAccessLevel accessLevel) {
		GitlabGroupMember ret;
		try {
			ret = api.addGroupMember(group, user, accessLevel);
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
	public void deleteGroupMember(GitlabGroup group, GitlabUser user) {
		try {
			api.deleteGroupMember(group, user);
		} catch (IOException e) {
			LOGGER.error("Exception caught while interrogating GitLab", e);
			throw new GitSyncException(e);
		}
	}

	/**
	 * Wrapper around {@link GitlabAPI#getUser()}.
	 * Removes the checked exception.
	 */
	public GitlabUser getUser() {
		GitlabUser ret;
		try {
			ret = api.getUser();
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
	public List<GitlabUser> getUsers() {
		List<GitlabUser> ret;
		try {
			ret = api.getUsers();
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
	public GitlabUser updateUser(
			Integer targetUserId,
			String email, String password, String username,
			String fullName, String skypeId, String linkedIn,
			String twitter, String website_url, Integer projects_limit,
			String extern_uid, String extern_provider_name,
			String bio, Boolean isAdmin, Boolean can_create_group) {
		GitlabUser ret;
		try {
			ret = api.updateUser(targetUserId,
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

}
