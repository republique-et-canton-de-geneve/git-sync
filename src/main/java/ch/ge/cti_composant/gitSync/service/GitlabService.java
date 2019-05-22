package ch.ge.cti_composant.gitSync.service;

import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;
import org.gitlab.api.models.GitlabVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for GitLab operations.
 */
public class GitlabService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitlabService.class);

	/**
	 * Constructs the in-memory GitLab tree (groups and users) from the specified LDAP tree.
	 * @return the GitLab tree, <b>restricted to the elements that come from the LDAP server</b>.
	 */
	public Gitlab buildGitlabContext(String hostname, String apiToken, LdapTree ldapTree) {
		// log on to GitLab
		LOGGER.info("Logging to the GitLab server");
		GitlabAPIWrapper api = new GitlabAPIWrapper(GitlabAPI.connect(hostname, apiToken));

		// create the missing groups on GitLab
		LOGGER.info("Creating the missing GitLab groups");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLdapGroupAdmin(ldapGroup))
				.filter(ldapGroup -> !MissionUtils.validateGitlabGroupExistence(ldapGroup, api))
				.forEach(ldapGroup -> {
					LOGGER.info("  Group [{}] does not exist in GitLab: creating it", ldapGroup.getName());
					createGroup(ldapGroup, api);
				});

		// retrieve the GitLab groups
		LOGGER.info("Retrieving the GitLab groups");
		List<GitlabGroup> groups;
		groups = api.getGroups();

		// check and store the GitLab groups in memory, including their users
		LOGGER.info("Constructing the tree of GitLab groups and users");
		Map<GitlabGroup, Map<String, GitlabUser>> tree = new HashMap<>();
		groups.stream()
				// exclude the groups created independently of LDAP
				.filter(gitlabGroup -> MissionUtils.validateLdapGroupExistence(gitlabGroup, ldapTree))
				// make sure the technical account owns the group
				.filter(gitlabGroup -> MissionUtils.validateGitlabGroupOwnership(gitlabGroup, api))
				.forEach(gitlabGroup -> {
					tree.put(gitlabGroup, new HashMap<>());
					api.getGroupMembers(gitlabGroup)
						.forEach(user -> tree.get(gitlabGroup).put(user.getUsername(), user));
				});

		return new Gitlab(tree, hostname, apiToken);
	}

	private void createGroup(LdapGroup ldapGroup, GitlabAPIWrapper api) {
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		api.createGroup(createGroupRequest, api.getUser());
	}

	private static boolean isLdapGroupAdmin(LdapGroup group) {
		return group.getName().equals(MissionUtils.getAdministratorGroup());
	}

}
