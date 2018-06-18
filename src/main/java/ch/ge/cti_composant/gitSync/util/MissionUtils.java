package ch.ge.cti_composant.gitSync.util;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser;
import org.apache.log4j.Logger;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cette classe contient les utilitaires principaux pour les missions.
 */
public class MissionUtils {

	static Logger log = Logger.getLogger(MissionUtils.class.getName());

	//<editor-fold desc="Méthodes concernant les groupes">

	/**
	 * Vérifie que nous sommes bien le propriétaire du groupe Gitlab en question.
	 *
	 * @param gitlabGroup Le groupe Gitlab en question
	 * @param gitlabAPI   L'API Gitlab
	 * @return Vrai si le groupe nous appartient, faux sinon.
	 * @apiNote Il ne peut y avoir selon GitLab qu'un seul propriétaire.
	 */
	public static boolean validateGitlabGroupOwnership(GitlabGroup gitlabGroup, GitlabAPI gitlabAPI) {
		try {
			GitlabGroupMember owner = gitlabAPI.getGroupMembers(gitlabGroup).stream()
					.filter(gitlabGroupMember -> gitlabGroupMember.getAccessLevel() == GitlabAccessLevel.Owner)
					.collect(Collectors.toList()).get(0);
			return gitlabAPI.getUser().getUsername().equals(owner.getUsername());
		} catch (IOException e) {
			log.error("Impossible d'obtenir des informations sur le groupe " + gitlabGroup.getName() + ".");
		}
		return false;
	}

	/**
	 * Vérifie que ce groupe existe bien dans LDAP.
	 *
	 * @param gitlabGroup Le groupe Gitlab
	 * @param ldapTree    L'arborescence LDAP
	 * @return Vrai si le groupe existe dans LDAP, faux sinon.
	 */
	public static boolean validateLDAPGroupExistence(GitlabGroup gitlabGroup, LDAPTree ldapTree) {
		return ldapTree.getGroups().contains(new LDAPGroup(gitlabGroup.getName()));
	}

	/**
	 * Valide l'existence du groupe Gitlab à partir d'un groupe LDAP.
	 *
	 * @param ldapGroup Le groupe LDAP.
	 * @param api       L'API Gitlab.
	 * @return Vrai si le groupe existe, faux sinon.
	 */
	public static boolean validateGitlabGroupExistence(LDAPGroup ldapGroup, GitlabAPI api) {
		try {
			api.getGroup(ldapGroup.getName());
			log.debug("Le groupe LDAP " + ldapGroup.getName() + " existe dans gitlab...");
			return true;
		} catch (IOException e) {
			log.debug("Le groupe LDAP " + ldapGroup.getName() + " n'existe pas dans GitLab.");
		}
		return false;
	}

	//</editor-fold>

	//<editor-fold desc="Méthodes concernant les utilisateurs">

	/**
	 * Vérifie que l'utilisateur existe bel et bien dans GitLab.
	 *
	 * @param user Un utilisateur LDAP
	 * @param users Les utilisateurs Gitlab.
	 * @return Vrai si l'utilisateur existe dans GitLab, faux sinon.
	 */
	public static boolean validateGitlabUserExistence(LDAPUser user, List<GitlabUser> users) {
		long usersCount = users.stream()
				.filter(gitlabUser -> gitlabUser.getUsername().equals(user.getName()))
				.count();
		switch ((int) usersCount) {
			case 1:
				return true;
			case 0:
				return false;
			default:
				throw new IllegalStateException("Plus d'un utilisateur avec le nom " + user.getName() + " ont été détectés.");
		}
	}

	/**
	 * Détermine si l'utilisateur a des droits admin. Il y a plusieurs critères à vérifier.
	 *
	 * @param user     Le membre du groupe
	 * @param api      L'objet général GitLab
	 * @param ldapTree L'arbre représentant l'arborescence LDAP.
	 * @return Vrai si l'utilisateur est admin, faux sinon.
	 */
	public static boolean isGitlabUserAdmin(GitlabUser user, GitlabAPI api, LDAPTree ldapTree) {
		try {
			// S'agit-il de "moi" ?
			boolean isTechnicalAccount = user.getUsername().equals(api.getUser().getUsername());
			boolean isTrivialAdmin = user.isAdmin();
			// Ne serait-il pas par hasard dans le groupe LDAP admin ?
			boolean isLDAPAdmin = ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(user.getUsername());
			return isLDAPAdmin || isTechnicalAccount || isTrivialAdmin;
		} catch (IOException e) {
			log.error("Erreur pendant l'évaluation des privilèges de l'utilisateur " + user.getUsername());
		}
		return false;
	}

	public static Map<String, GitlabUser> getAllGitlabUsers(GitlabAPI api) {
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));
			return allUsers;
		} catch (IOException e) {
			log.error("Impossible de récupérer tous les utilisateurs. L'erreur était : " + e);
		}
		return new HashMap<>();
	}

	public static boolean isGitlabUserMemberOfGroup(List<GitlabGroupMember> members, String user){
		return members.stream().filter(member -> member.getUsername().equals(user)).count() == 1;
	}

	public static GitlabUser getGitlabUser(GitlabAPI api, String username) {
			return getAllGitlabUsers(api).get(username);
	}

	//</editor-fold>

}
