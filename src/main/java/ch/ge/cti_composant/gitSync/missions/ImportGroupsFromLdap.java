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
 * Classe responsable de la création des groupes GitLab selon le ldap.
 *
 * @implNote Cette classe ne supprime PAS les groupes si pas trouvés dans ldap.
 * Le sens de synchronisation est donc TOUJOURS ldap (groupes existants) -> GitLab.
 */
public class ImportGroupsFromLdap implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLdap.class);

	/**
	 * Crée les groupes dans GitLab.
	 *
	 * @implNote Le/s groupe/s considérés comme admins n'auront pas de groupe créé.
	 */
	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : Groupes ldap � groupes GitLab");
		ldapTree.getGroups().stream()
				.filter(ldapGroup -> !isLdapGroupAdmin(ldapGroup.getName()))
				.forEach(ldapGroup -> {
					if (MissionUtils.validateGitlabGroupExistence(ldapGroup, gitlab.getApi())) {
						LOGGER.info("Le groupe [{}] existe : rien ne sera fait", ldapGroup.getName());
					} else {
						LOGGER.info("Groupe inexistant sur GitLab detecte : [{}]. Creation en cours...", ldapGroup.getName());
						createGroup(ldapGroup, gitlab);
					}
				});
		LOGGER.info("Synchronisation terminee");
	}

	private void createGroup(LdapGroup ldapGroup, Gitlab gitlab) {
		// Création du groupe
		CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
		createGroupRequest.setVisibility(GitlabVisibility.PRIVATE);
		try {
			gitlab.getApi().createGroup(createGroupRequest, gitlab.getApi().getUser());
		} catch (IOException e) {
			LOGGER.error("Impossible de creer le groupe [{}] : {}", ldapGroup.getName(), e);
		}
	}

	private static boolean isLdapGroupAdmin(String role) {
		return role.equals(MiscConstants.ADMIN_LDAP_GROUP);
	}

}
