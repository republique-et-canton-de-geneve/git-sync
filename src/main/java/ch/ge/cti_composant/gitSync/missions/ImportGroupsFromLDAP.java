package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabVisibility;

import java.io.IOException;

/**
 * Classe responsable de la création des groupes GitLab selon le LDAP.
 *
 * @implNote Cette classe ne supprime PAS les groupes si pas trouvés dans LDAP.
 * Le sens de synchronisation est donc TOUJOURS LDAP (groupes existants) -> GitLab.
 */
public class ImportGroupsFromLDAP implements Mission {
	Logger log = Logger.getLogger(ImportGroupsFromLDAP.class.getName());

	/**
	 * Crée les groupes dans GitLab.
	 *
	 * @implNote Le/s groupe/s considérés comme admins n'auront pas de groupe créé.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : Groupes LDAP à groupes GitLab");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLDAPGroupAdmin(ldapGroup.getName()))
				.forEach(ldapGroup -> {
					if (MissionUtils.validateGitlabGroupExistence(ldapGroup, gitlab.getApi())) {
						log.debug("Le groupe " + ldapGroup.getName() + " existe. Rien ne sera fait.");
					} else {
						log.info("Groupe inexistant sur GitLab détecté : " + ldapGroup.getName() + " ! Création en cours...");
						createGroup(ldapGroup, gitlab);
					}
				});
		log.info("Synchronisation terminée.");
	}

	private void createGroup(LDAPGroup ldapGroup, Gitlab gitlab) {
		// Création du groupe
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		try {
			gitlab.getApi().createGroup(createGroupRequest, gitlab.getApi().getUser());
		} catch (IOException e) {
			log.fatal("Impossible de créer le groupe " + ldapGroup.getName() + " : " + e);
		}
	}

	private static boolean isLDAPGroupAdmin(String role) {
		return role.equals(MiscConstants.ADMIN_LDAP_GROUP);
	}
}
