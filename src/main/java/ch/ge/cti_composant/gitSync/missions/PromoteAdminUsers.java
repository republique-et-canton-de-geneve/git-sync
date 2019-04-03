package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Adds the Admin users if they are present in the LDAP group.
 */
public class PromoteAdminUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PromoteAdminUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: adding admin users");
		try{
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			ldapTree.getUsers(new LdapGroup(MiscConstants.ADMIN_LDAP_GROUP)).forEach((username, ldapUser) -> {
				boolean doesUserExist = MissionUtils.validateGitlabUserExistence(ldapUser, new ArrayList<>(allUsers.values()));

				if (doesUserExist && !allUsers.get(username).isAdmin()) {
					LOGGER.info("Setting user [{}] as administrator", username);
					try {
						gitlab.getApi().updateUser(
								allUsers.get(username).getId(), allUsers.get(username).getEmail(), null,
								null, null, null, null, null, null,
								null, null, null, null, true, null);
					} catch (IOException e) {
						LOGGER.error("Exception caught while setting user [{}] as administrator", username);
					}
				} else if (doesUserExist &&  MissionUtils.isGitlabUserAdmin(allUsers.get(username), gitlab.getApi(), ldapTree)){
					LOGGER.info("User [{}] is already administrator", username);
				} else {
					LOGGER.info("User [{}] won't be set as adminsitrator as it does not exist in GitLab", username);
				}
			});
		} catch (IOException e){
			LOGGER.error("Exception caught while retrieving the list of admin users", e);
		}

		LOGGER.info("Mapping completed");
	}

}
