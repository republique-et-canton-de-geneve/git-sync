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

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupParams;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * A simple wrapper around a {@link GitLabApi} object, to replace the checked exceptions with unchecked exceptions.
 * Not all methods of class GitLabAPI are overridden - only those relevant to this application.
 */
public class GitlabAPIWrapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabAPIWrapper.class);

	private static final String ERROR_MESSAGE = "Exception caught while interrogating GitLab";

	private static final int HTTP_STATUS_NOT_FOUND = 404;

	private final int maxTries = GitSync.getPropertyAsInt("retry-nb-max-attempts", 3);
	private final long sleepTime = GitSync.getPropertyAsInt("retry-time-between-attemps", 5000);

	/**
	 * The wrapped GitLabApi object.
	 */
	private final GitLabApi api;

	/**
	 * Constructor.
	 */
	public GitlabAPIWrapper(GitLabApi api) {
		this.api = api;
	}

	/**
	 * Wrapper around {@link GitLabApi#getGroupApi()#getGroups()} .
	 * Removes the checked exception.
	 */
	public List<Group> getGroups() {
		return run(() -> api.getGroupApi().getGroups());
	}

	/**
	 * Wrapper around {@link GitLabApi#getGroupApi()#getGroup(String)} .
	 * Removes the checked exception and returns null (instead of throwing an exception) if the group is not found.
	 */
	public Group getGroup(String path) {
		return run(() -> {
			try {
				return api.getGroupApi().getGroup(path);
			} catch (GitLabApiException e) {
				if (e.getHttpStatus() == HTTP_STATUS_NOT_FOUND) {
					return null; // Group not found
				}
				throw e;
			}
		});
	}

	public Group createGroup(GroupParams groupParams) {
		return runOnlyIfNotDryRun(() -> api.getGroupApi().createGroup(groupParams));
	}

	public List<Member> getGroupMembers(Group group) {
		return run(() -> api.getGroupApi().getMembers(group.getId()));
	}

	public Member addGroupMember(Group group, Long userId, AccessLevel accessLevel) {
		return runOnlyIfNotDryRun(() -> api.getGroupApi().addMember(group, userId, accessLevel));
	}

	public void deleteGroupMember(Group group, Long userId) {
		runOnlyIfNotDryRun(() -> {
			api.getGroupApi().removeMember(group.getId(), userId);
			return null;
		});
	}

	public User getUser() {
		return run(() -> api.getUserApi().getCurrentUser());
	}

	/**
	 * Wrapper around {@link GitLabApi#getUserApi()#getUsers()}.
	 * Removes the checked exception.
	 */
	public List<User> getUsers() {
		return run(() -> api.getUserApi().getUsers());
	}

	public void promoteToAdmin(Long targetUserId) {
		runOnlyIfNotDryRun(() -> {
			User user = api.getUserApi().getUser(targetUserId);
			user.setIsAdmin(true);
			api.getUserApi().updateUser(user, null);
			return null;
		});
	}

	public void blockUser(Long targetUserId) {
		runOnlyIfNotDryRun(() -> {
			api.getUserApi().blockUser(targetUserId);
			return null;
		});
	}

	public void unblockUser(Long targetUserId) {
		runOnlyIfNotDryRun(() -> {
			api.getUserApi().unblockUser(targetUserId);
			return null;
		});
	}

	private void sleep(int count) {
		try {
			Thread.sleep(sleepTime * count);
		} catch (InterruptedException e1) {
			LOGGER.warn(e1.getMessage());
			Thread.currentThread().interrupt();
		}
	}

	private void handleException(GitLabApiException e, int count) throws GitSyncException {
		if (count >= maxTries) {
			LOGGER.warn("attempt {}/{} failed, throw exception", count, maxTries);
			LOGGER.error(ERROR_MESSAGE, e);
			throw new GitSyncException(e);
		} else {
			LOGGER.warn("attempt {}/{} failed, retry => {}", count, maxTries, e.getMessage());
			sleep(count);
		}
	}

	private <T> T runOnlyIfNotDryRun(Callable<T> callable) {
		if (!GitSync.isDryRun()) {
			return run(callable);
		}
		return null;
	}

	private <T> T run(Callable<T> callable) {
		int count = 0;
		while (true) {
			try {
				return callable.call();
			} catch (GitLabApiException e) {
				count++;
				handleException(e, count);
			} catch (Exception e) {
				throw new GitSyncException(e);
			}
		}
	}

}
