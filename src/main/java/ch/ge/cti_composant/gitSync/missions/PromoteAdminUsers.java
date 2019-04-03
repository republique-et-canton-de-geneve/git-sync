package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Ajoute les utilisateurs en admin si ils sont dans le groupe ldap.
 */
public class PromoteAdminUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PromoteAdminUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : ajout des admins...");
		try{
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			ldapTree.getUsers(new LdapGroup(MiscConstants.ADMIN_LDAP_GROUP)).forEach((username, ldapUser) -> {
				boolean doesUserExist = MissionUtils.validateGitlabUserExistence(ldapUser, new ArrayList<>(allUsers.values()));

				if (doesUserExist && !allUsers.get(username).isAdmin()) {
					LOGGER.info("Ajout de l'utilisateur [{}] en admin", username);
					try {
						gitlab.getApi().updateUser(
								allUsers.get(username).getId(), allUsers.get(username).getEmail(), null,
								null, null, null, null, null, null,
								null, null, null, null, true, null);
					} catch (IOException e) {
						LOGGER.error("Impossible d'ajouter [{}] en administrateur", username);
					}
				} else if (doesUserExist &&  MissionUtils.isGitlabUserAdmin(allUsers.get(username), gitlab.getApi(), ldapTree)){
					LOGGER.info("L'utilisateur [{}] est deja admin", username);
				} else {
					LOGGER.info("L'utilisateur [{}] ne sera pas ajoute en admin car il n'est pas dans GitLab", username);
				}
			});
		} catch (IOException e){
			LOGGER.error("Impossible de recuperer la liste des utilisateurs admin : {}", e);
		}

		LOGGER.info("Synchronisation terminee");
	}

}
