package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabGroup;

import java.io.IOException;

/**
 * Classe responsable de la suppression des droits "en trop" sur GitLab.
 *
 * @implNote Les utilisateurs admins sont ignorés. Ils peuvent aller dans n'importe
 * quel type de groupe ou de projet.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {
	Logger log = Logger.getLogger(ImportGroupsFromLDAP.class.getName());

	/**
	 * Synchronise les utilisateurs existants Gitlab sur le LDAP.
	 *
	 * @param ldapTree l'arbre LDAP
	 * @param gitlab   Le contexte GitLab.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : synchronisation des utilisateurs avec le LDAP.");
		// Pour chaque groupe...
		gitlab.getTree().getGroups()
				.forEach(gitlabGroup -> {
					log.info("Synchronisation du groupe " + gitlabGroup.getName() + " en cours.");
					handleGroup(gitlabGroup, ldapTree, gitlab);
				});

		log.info("Synchronisation terminée.");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LDAPTree ldapTree, Gitlab gitlab) {
		LDAPGroup ldapGroup = new LDAPGroup(gitlabGroup.getName());

		// Pour chaque utilisateur...
		try {
			gitlab.getApi().getGroupMembers(gitlabGroup.getId()).stream()
					.filter(gitlabGroupMember -> !MissionUtils.isGitlabUserAdmin(gitlabGroupMember, gitlab.getApi(), ldapTree))
					.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
					.forEach(member -> {
					    	if (!MiscConstants.FISHEYE_USERNAME.equals(member.getUsername())) {
    						log.info("L'utilisateur " + member.getUsername() + " n'a pas/plus les permissions pour le rôle " + gitlabGroup.getName() + ".");
    						try {
    							gitlab.getApi().deleteGroupMember(gitlabGroup, member);
    						} catch (IOException e) {
    							log.error("Une erreur est survenue lors de la suppression du rôle " + gitlabGroup.getName() + " pour " + member.getUsername() + ".");
    						}
					    }
					});
		} catch (IOException e) {
			log.error("Une erreur est survenue lors de la détection du groupe " + gitlabGroup.getName() + " : " + e);
		}

	}

}
