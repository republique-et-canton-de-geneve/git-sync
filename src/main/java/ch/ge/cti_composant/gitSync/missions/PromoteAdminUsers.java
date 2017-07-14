package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Ajoute les utilisateurs en admin si ils sont dans le groupe LDAP.
 */
public class PromoteAdminUsers implements Mission {
	Logger log = Logger.getLogger(PromoteAdminUsers.class.getName());

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : ajout des admins.");
		try{
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			ldapTree.getUsers(new LDAPGroup(MiscConstants.ADMIN_LDAP_GROUP)).forEach((username, ldapUser) -> {
				boolean doesUserExist = MissionUtils.validateGitlabUserExistence(ldapUser, new ArrayList<>(allUsers.values()));

				if (doesUserExist && !allUsers.get(username).isAdmin()) {
					log.warn("Ajout de l'utilisateur " + username + " en admin.");
					try {
						gitlab.getApi().updateUser(
								allUsers.get(username).getId(), allUsers.get(username).getEmail(), null,
								null, null, null, null, null, null,
								null, null, null, null, true, null);
					} catch (IOException e) {
						log.error("Impossible d'ajouter " + username + " en administrateur.");
					}
				} else if (doesUserExist &&  MissionUtils.isGitlabUserAdmin(allUsers.get(username), gitlab.getApi(), ldapTree)){
					log.debug(username + " est déjà admin.");
				} else {
					log.debug(username + " ne sera pas ajouté en admin car il n'est pas dans GitLab.");
				}
			});
		} catch (IOException e){
			log.error("Impossible de récupérer la liste des utilisateurs admin. L'erreur était : " + e);
		}

		log.info("Synchronisation terminée.");
	}
}
