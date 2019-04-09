package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the GitLab groups.
 * <br/>
 * Does not remove the GitLab groups that has not been foud in the LDAP server.
 * The direction of the mapping is always one way: LDAP (existing groups) -> GitLab.
 */
public class ImportGroupsFromLdap implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLdap.class);

	/**
	 * Creates the groups in GitLab.
	 * <br>>
	 * No group is created for the admin groups.
	 */
	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping : LDAP groups to GitLab groups");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLdapGroupAdmin(ldapGroup))
				.forEach(ldapGroup -> {
					if (MissionUtils.validateGitlabGroupExistence(ldapGroup, gitlab.getApi())) {
						LOGGER.info("    Group [{}] exists: no op", ldapGroup.getName());
					} else {
						LOGGER.info("    Group [{}] does not exist in GitLab : creating it", ldapGroup.getName());
						createGroup(ldapGroup, gitlab);
					}
				});
		LOGGER.info("Mapping completed");
	}

	private void createGroup(LdapGroup ldapGroup, Gitlab gitlab) {
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		gitlab.apiCreateGroup(createGroupRequest, gitlab.apiGetUser());
	}

	private static boolean isLdapGroupAdmin(LdapGroup group) {
		return group.getName().equals(MissionUtils.getAdministratorGroup());
	}

}
