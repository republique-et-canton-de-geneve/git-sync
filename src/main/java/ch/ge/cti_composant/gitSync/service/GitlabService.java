package ch.ge.cti_composant.gitSync.service;

import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for GitLab operations.
 */
public class GitlabService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabService.class);

	/**
	 * Constructs the GitLab tree (groups and users) from the specified ldap tree.
	 * @return the GitLab tree, <b>restricted to the elements that come from the ldap server</b>.
	 */
	public Gitlab buildGitlabContext(String hostname, String apiToken, LdapTree ldapTree) {
		// log on to GitLab
		GitlabAPI api = GitlabAPI.connect(hostname, apiToken);

		// retrieve the GitLab groups
		List<GitlabGroup> groups;
		try {
			groups = api.getGroups();
		} catch (IOException e) {
			LOGGER.error("Exception caught while retrieving the GitLab groups", e);
			throw new GitSyncException(e);
		}

		// check and store the GitLab groups, including their users
		Map<GitlabGroup, Map<String, GitlabUser>> tree = new HashMap<>();
		groups.stream()
				// exclude the groups created by the user
				.filter(gitlabGroup -> MissionUtils.validateLdapGroupExistence(gitlabGroup, ldapTree))
				// make sure the technical account owns the group
				.filter(gitlabGroup -> MissionUtils.validateGitlabGroupOwnership(gitlabGroup, api))
				.forEach(gitlabGroup -> {
					tree.put(gitlabGroup, new HashMap<>());
					try {
						api.getGroupMembers(gitlabGroup).forEach(user -> tree.get(gitlabGroup).put(user.getUsername(), user));
					} catch (IOException e) {
						LOGGER.error("Exception caught while mapping group [{}] : {}", gitlabGroup.getName(), e);
					}
				});

		return new Gitlab(tree, hostname, apiToken);
	}

}
