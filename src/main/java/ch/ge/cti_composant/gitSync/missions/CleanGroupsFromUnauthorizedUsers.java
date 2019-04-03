package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.GitlabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Classe responsable de la suppression des droits "en trop" sur GitLab.
 *
 * @implNote Les utilisateurs admins sont ignorés. Ils peuvent aller dans n'importe
 * quel type de groupe ou de projet.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLDAP_temp.class);

	/**
	 * Synchronise les utilisateurs existants GitlabContext sur le ldap.
	 *
	 * @param ldapTree l'arbre ldap
	 * @param gitlab   Le contexte GitLab.
	 */
	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : synchronisation des utilisateurs avec le ldap");

		// Pour chaque groupe...
		gitlab.getGroups()
			.forEach(gitlabGroup -> {
				LOGGER.info("Synchronisation du groupe [{}] en cours", gitlabGroup.getName());
				handleGroup(gitlabGroup, ldapTree, gitlab);
			});

		LOGGER.info("Synchronisation terminee");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LdapTree ldapTree, Gitlab gitlab) {
		LdapGroup ldapGroup = new LdapGroup(gitlabGroup.getName());

		// Pour chaque utilisateur...
		try {
			gitlab.getApi().getGroupMembers(gitlabGroup.getId()).stream()
					.filter(gitlabGroupMember -> !MissionUtils.isGitlabUserAdmin(gitlabGroupMember, gitlab.getApi(), ldapTree))
					.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
					.forEach(member -> {
					    	if ((!MiscConstants.FISHEYE_USERNAME.equals(member.getUsername())) && (!MiscConstants.MWFL_USERNAME.equals(member.getUsername()))) {
    						LOGGER.info("L'utilisateur " + member.getUsername() + " n'a pas/plus les permissions pour le r�le " + gitlabGroup.getName());
    						try {
    							gitlab.getApi().deleteGroupMember(gitlabGroup, member);
    						} catch (IOException e) {
    							LOGGER.error("Une erreur est survenue lors de la suppression du r�le " + gitlabGroup.getName() + " pour " + member.getUsername() );
    						}
					    }
					});
		} catch (IOException e) {
			LOGGER.error("Une erreur est survenue lors de la detection du groupe [{}] : {}", gitlabGroup.getName(), e);
		}
	}

}
