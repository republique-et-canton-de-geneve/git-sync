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
package ch.ge.cti_composant.gitsync.util.gitlab;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabAbstractMember;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;

/**
 * A simple wrapper around a {@link GitlabAPI} object, to replace the checked exceptions with unchecked exceptions.
 * Not all methods of class GitLabAPI are overridden - only those relevant to this application.
 */
public class GitlabAPIWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabAPIWrapper.class);

	private static final String ERROR_MESSAGE = "Exception caught while interrogating GitLab";

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
			LOGGER.error(ERROR_MESSAGE, e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#getGroup(String)} .
	 * Removes the checked exception and returns null (instead of throwing an exception) if the group is not found.
	 */
	public GitlabGroup getGroup(String path) {
		GitlabGroup ret = null;
		try {
			ret = api.getGroup(path);
		} catch (FileNotFoundException e) {
			// occurs when the group does not exist in GitLab
		} catch (IOException e) {
			LOGGER.error(ERROR_MESSAGE, e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#createGroup(CreateGroupRequest, GitlabUser)}.
	 * Removes the checked exception.
	 */
	public GitlabGroup createGroup(CreateGroupRequest request, GitlabUser sudoUser) {
		GitlabGroup ret = null;
		if(!GitSync.isDryRun()) {
        		try {
        			ret = api.createGroup(request, sudoUser);
        		} catch (IOException e) {
        			LOGGER.error(ERROR_MESSAGE, e);
        			throw new GitSyncException(e);
        		}
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
			LOGGER.error(ERROR_MESSAGE, e);
			throw new GitSyncException(e);
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#addGroupMember(GitlabGroup, GitlabUser, GitlabAccessLevel)}.
	 * Removes the checked exception.
	 */
	public GitlabGroupMember addGroupMember(GitlabGroup group, GitlabUser user, GitlabAccessLevel accessLevel) {
		GitlabGroupMember ret = null;
		if(!GitSync.isDryRun()) {
        		try {
        			ret = api.addGroupMember(group, user, accessLevel);
        		} catch (IOException e) {
        			LOGGER.error(ERROR_MESSAGE, e);
        			throw new GitSyncException(e);
        		}
		}
		return ret;
	}

	/**
	 * Wrapper around {@link GitlabAPI#deleteGroupMember(GitlabGroup, GitlabUser)}.
	 * Removes the checked exception.
	 * Gitlab api bug : too many slash before members in api.deleteGroupMember(group, user);
	 */
	public void deleteGroupMember(GitlabGroup group, GitlabUser user) {
		if(!GitSync.isDryRun()) {
        		try {
        		        String tailUrl = GitlabGroup.URL + "/" + group.getId() + GitlabAbstractMember.URL + "/" + user.getId();
        		        api.retrieve().method("DELETE").to(tailUrl, Void.class);
        		} catch (IOException e) {
        			LOGGER.error(ERROR_MESSAGE, e);
        			throw new GitSyncException(e);
        		}
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
			LOGGER.error(ERROR_MESSAGE, e);
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
			LOGGER.error(ERROR_MESSAGE, e);
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
			String twitter, String websiteUrl, Integer projectsLimit,
			String externUid, String externProviderName,
			String bio, Boolean isAdmin, Boolean canCreateGroup) {
		GitlabUser ret = null;
		if(!GitSync.isDryRun()) {
        		try {
        			ret = api.updateUser(targetUserId,
        					email, password, username,
        					fullName, skypeId, linkedIn,
        					twitter, websiteUrl, projectsLimit,
        					externUid, externProviderName,
        					bio, isAdmin, canCreateGroup);
        		} catch (IOException e) {
        			LOGGER.error(ERROR_MESSAGE, e);
        			throw new GitSyncException(e);
        		}
		}
		return ret;
	}

    /**
     * Wrapper around {@link GitlabAPI#blockUser(Integer)}. Removes the checked exception.
     */
    public void blockUser(Integer targetUserId) {
	if (!GitSync.isDryRun()) {
	    try {
		api.blockUser(targetUserId);
	    }
	    catch (IOException e) {
		LOGGER.error(ERROR_MESSAGE, e);
		throw new GitSyncException(e);
	    }
	}
    }

    /**
     * Wrapper around {@link GitlabAPI#unblockUser(Integer)}. Removes the checked exception.
     */
    public void unblockUser(Integer targetUserId) {
	if (!GitSync.isDryRun()) {
	    try {
		api.unblockUser(targetUserId);
	    }
	    catch (IOException e) {
		LOGGER.error(ERROR_MESSAGE, e);
		throw new GitSyncException(e);
	    }
	}
    }

}
