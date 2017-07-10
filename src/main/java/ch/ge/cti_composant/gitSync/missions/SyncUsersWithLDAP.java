package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Bloque les utilisateurs qui ne sont plus dans le LDAP.
 */
public class SyncUsersWithLDAP implements Mission {
	Logger log = Logger.getLogger(ImportGroupsFromLDAP.class.getName());

	/**
	 * Synchronise les utilisateurs existants Gitlab sur le LDAP.
	 * Procédure : (PC = Pour Chaque) PC Groupe Gitlab, PC Utilisateur du groupe, existe dans role LDAP du meme nom ?
	 * si pas => exit groupe SAUF si utilisateur administrateur, sinon pas d'action
	 *
	 * @param ldapTree l'arbre LDAP
	 * @param gitlab   Le contexte GitLab.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : synchronisation des utilisateurs avec le LDAP.");
		// Pour chaque groupe...
		gitlab.getTree().getGroups().stream().filter(gitlabGroup -> amITheOwnerOfThisGroup(gitlabGroup, gitlab.getApi()))
				.forEach(gitlabGroup -> {
					log.info("Synchronisation du groupe " + gitlabGroup.getName() + " en cours.");
					handleGroupForUser(gitlabGroup, ldapTree, gitlab);
				});

		log.info("Synchronisation terminée.");
	}

	private boolean amITheOwnerOfThisGroup(GitlabGroup gitlabGroup, GitlabAPI gitlab) {
		try {
			return gitlab.getGroupMembers(gitlabGroup).stream().filter(member -> {
				try {
					return member.getUsername().equals(gitlab.getUser().getUsername()) && member.getAccessLevel() == GitlabAccessLevel.Owner;
				} catch (IOException e) {
					return false;
				}
			}).collect(Collectors.toList()).size() >= 0;
		} catch (IOException e) {
			log.error("Une erreur s'est produite lors de l'évaluation d'appartenance au groupe suivant : " + gitlabGroup.getName() + ". L'erreur était : " + e);
			return false;
		}
	}

	private void handleGroupForUser(GitlabGroup gitlabGroup, LDAPTree ldapTree, Gitlab gitlab){
		LDAPGroup ldapGroup = new LDAPGroup(gitlabGroup.getName());

		// Le groupe existe-t'il dans LDAP ? Et est-ce que le compte technique est bien le owner du groupe ?
		if (ldapTree.getGroups().contains(ldapGroup)) {
			// Pour chaque utilisateur...
			gitlab.getTree().getUsers(gitlabGroup).forEach((username, gitlabUser) -> {
				log.info("Vérification de l'utilisateur " + username + " dans le groupe " + gitlabGroup.getName() + "...");
				if (ldapTree.getUsers(ldapGroup).containsKey(username)) {
					log.debug("L'utilisateur " + username + " a bien le rôle " + ldapGroup.getName() + ".");
				} else if (!ldapTree.getUsers(ldapGroup).containsKey(username) && ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(username)) {
					log.info("L'utilisateur " + username + " n'a pas les droits pour le groupe " + gitlabGroup.getName() + " mais est admin.");
				} else {
					log.warn("Attention ! L'utilisateur " + username + " n'a plus le rôle " + ldapGroup.getName() + ". Suppression des permissions...");
					try {
						gitlab.getApi().deleteGroupMember(gitlabGroup, gitlabUser);
					} catch (IOException e) {
						log.error("Une erreur s'est produite lors de la suppression de l'utilisateur " + username +
								" du groupe " + gitlabGroup.getName() + ". L'erreur était : " + e);
					}
				}
			});
		} else {
			log.warn("Le groupe " + gitlabGroup.getName() + " n'est pas un groupe LDAP.");
		}
	}
}
