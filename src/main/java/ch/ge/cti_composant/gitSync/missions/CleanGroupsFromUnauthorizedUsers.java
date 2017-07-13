package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vide les groupes des utilisateurs dont l'autorisation a été révoquée.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {
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
					return member.getId().equals(gitlab.getUser().getId()) && member.getAccessLevel() == GitlabAccessLevel.Owner;
				} catch (IOException e) {
					return false;
				}
			}).collect(Collectors.toList()).size() == 1;
		} catch (IOException e) {
			log.error("Une erreur s'est produite lors de l'évaluation d'appartenance au groupe suivant : " + gitlabGroup.getName() + ". L'erreur était : " + e);
			return false;
		}
	}

	private void handleGroupForUser(GitlabGroup gitlabGroup, LDAPTree ldapTree, Gitlab gitlab) {
		LDAPGroup ldapGroup = new LDAPGroup(gitlabGroup.getName());

		// Le groupe existe-t'il dans LDAP ?
		if (ldapTree.getGroups().contains(ldapGroup)) {
			// Pour chaque utilisateur...
			try {
				List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
				// GITLAB conformément à LDAP
				for (GitlabGroupMember member : members) {
					log.debug("Vérification du rôle " + gitlabGroup.getName() + " pour " + member.getUsername() + "...");
					if (ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername())) {
						log.debug(member.getUsername() + " a bien les permissions pour le rôle " + gitlabGroup.getName());

					} else if (
							!ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()) &&
									(isUserAdmin(member, gitlab, ldapTree))) {
						log.debug("L'utilisateur " + member.getUsername() + " est admin. Ignoré.");
					} else {
						log.info("L'utilisateur " + member.getUsername() + " n'a pas/plus les permissions pour le rôle " + gitlabGroup.getName() + ".");
						gitlab.getApi().deleteGroupMember(gitlabGroup, member);
					}
				}
			} catch (IOException e) {
				log.error("Une erreur est survenue lors de la détection du groupe " + gitlabGroup.getName() + " : " + e);
			}
		} else {
			log.warn("Le groupe " + gitlabGroup.getName() + " n'est pas un groupe reconnu du LDAP.");
		}
	}

	private boolean isUserAdmin(GitlabGroupMember member, Gitlab gitlab, LDAPTree ldapTree){
		try{
			return member.getUsername().equals(gitlab.getApi().getUser().getUsername()) ||
					member.isAdmin() ||
					ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(member.getUsername());
		} catch (IOException e){
			log.error("Erreur pendant l'évaluation des privilèges de l'utilisateur " + member.getUsername());
		}
		return false;
	}
}
