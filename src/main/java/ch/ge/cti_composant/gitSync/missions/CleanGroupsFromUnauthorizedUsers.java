package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;

import org.gitlab.api.models.GitlabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * Classe responsable de la suppression des droits "en trop" sur GitLab.
 *
 * @implNote Les utilisateurs admins sont ignorÃ©s. Ils peuvent aller dans n'importe
 * quel type de groupe ou de projet.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLDAP.class);

	/**
	 * Synchronise les utilisateurs existants Gitlab sur le LDAP.
	 *
	 * @param ldapTree l'arbre LDAP
	 * @param gitlab   Le contexte GitLab.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : synchronisation des utilisateurs avec le LDAP");

		// Pour chaque groupe...
		gitlab.getTree().getGroups()
			.forEach(gitlabGroup -> {
				LOGGER.info("Synchronisation du groupe [{}] en cours", gitlabGroup.getName());
				handleGroup(gitlabGroup, ldapTree, gitlab);
			});

		LOGGER.info("Synchronisation terminee");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LDAPTree ldapTree, Gitlab gitlab) {
		LDAPGroup ldapGroup = new LDAPGroup(gitlabGroup.getName());

		// Pour chaque utilisateur...
		try {
			gitlab.getApi().getGroupMembers(gitlabGroup.getId()).stream()
					.filter(gitlabGroupMember -> !MissionUtils.isGitlabUserAdmin(gitlabGroupMember, gitlab.getApi(), ldapTree))
					.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
					.forEach(member -> {
					    	if ((!MiscConstants.FISHEYE_USERNAME.equals(member.getUsername())) && (!MiscConstants.MWFL_USERNAME.equals(member.getUsername()))) {
    						LOGGER.info("L'utilisateur " + member.getUsername() + " n'a pas/plus les permissions pour le rôle " + gitlabGroup.getName());
    						try {
    							gitlab.getApi().deleteGroupMember(gitlabGroup, member);
    						} catch (IOException e) {
    							LOGGER.error("Une erreur est survenue lors de la suppression du rôle " + gitlabGroup.getName() + " pour " + member.getUsername() );
    						}
					    }
					});
		} catch (IOException e) {
			LOGGER.error("Une erreur est survenue lors de la detection du groupe [{}] : {}", gitlabGroup.getName(), e);
		}
	}

}
