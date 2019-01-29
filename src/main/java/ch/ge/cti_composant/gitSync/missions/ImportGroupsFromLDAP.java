package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;

import org.gitlab.api.models.CreateGroupRequest;
import org.gitlab.api.models.GitlabVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * Classe responsable de la création des groupes GitLab selon le LDAP.
 *
 * @implNote Cette classe ne supprime PAS les groupes si pas trouvés dans LDAP.
 * Le sens de synchronisation est donc TOUJOURS LDAP (groupes existants) -> GitLab.
 */
public class ImportGroupsFromLDAP implements Mission {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLDAP.class);

	/**
	 * Crée les groupes dans GitLab.
	 *
	 * @implNote Le/s groupe/s considérés comme admins n'auront pas de groupe créé.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : Groupes LDAP � groupes GitLab");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLDAPGroupAdmin(ldapGroup.getName()))
				.forEach(ldapGroup -> {
					if (MissionUtils.validateGitlabGroupExistence(ldapGroup, gitlab.getApi())) {
						LOGGER.info("Le groupe " + ldapGroup.getName() + " existe : rien ne sera fait");
					} else {
						LOGGER.info("Groupe inexistant sur GitLab d�tect� : " + ldapGroup.getName() + " ! Cr�ation en cours...");
						createGroup(ldapGroup, gitlab);
					}
				});
		LOGGER.info("Synchronisation termin�e");
	}

	private void createGroup(LDAPGroup ldapGroup, Gitlab gitlab) {
		// Création du groupe
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		try {
			gitlab.getApi().createGroup(createGroupRequest, gitlab.getApi().getUser());
		} catch (IOException e) {
			LOGGER.error("Impossible de cr�er le groupe " + ldapGroup.getName() + " : " + e);
		}
	}

	private static boolean isLDAPGroupAdmin(String role) {
		return role.equals(MiscConstants.ADMIN_LDAP_GROUP);
	}
}
