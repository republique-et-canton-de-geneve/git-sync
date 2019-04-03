package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;

import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;

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
				.filter(ldapGroup -> !isLdapGroupAdmin(ldapGroup.getName()))
				.forEach(ldapGroup -> {
					if (MissionUtils.validateGitlabGroupExistence(ldapGroup, gitlab.getApi())) {
						LOGGER.info("Group [{}] exists: no op", ldapGroup.getName());
					} else {
						LOGGER.info("Detected group [{}] not existing in GitLab. Creating it", ldapGroup.getName());
						createGroup(ldapGroup, gitlab);
					}
				});
		LOGGER.info("Mapping completed");
	}

	private void createGroup(LdapGroup ldapGroup, Gitlab gitlab) {
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		try {
			gitlab.getApi().createGroup(createGroupRequest, gitlab.getApi().getUser());
		} catch (IOException e) {
			LOGGER.error("Exception caught while creating group [{}]", ldapGroup.getName(), e);
		}
	}

	private static boolean isLdapGroupAdmin(String group) {
		return group.equals(MiscConstants.ADMIN_LDAP_GROUP);
	}

}
