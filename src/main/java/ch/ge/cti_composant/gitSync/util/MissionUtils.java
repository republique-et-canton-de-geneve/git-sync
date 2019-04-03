package ch.ge.cti_composant.gitSync.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;

/**
 * Cette classe contient les utilitaires principaux pour les missions.
 */
public class MissionUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(MissionUtils.class);

	private MissionUtils() {
	}

	/**
	 * Checks that we are the Owner of the specified GitLab group.
	 * By "we", we mean "the GitLab (technical) user associated with the token used for the connection to GitLab".
	 * <br/>
	 * Note: in GitLab a group can have only one owner.
	 */
	public static boolean validateGitlabGroupOwnership(GitlabGroup gitlabGroup, GitlabAPI gitlabAPI) {
		try {
			for (GitlabGroupMember owner : gitlabAPI.getGroupMembers(gitlabGroup).stream()
					.filter(gitlabGroupMember -> gitlabGroupMember.getAccessLevel() == GitlabAccessLevel.Owner)
					.collect(Collectors.toList()))
			{
			    if (gitlabAPI.getUser().getUsername().equals(owner.getUsername())) {
					return true;
			    }
			}
			return false;
		} catch (IOException e) {
			LOGGER.error("Impossible d'obtenir des informations sur le groupe [{}]", gitlabGroup.getName());
		}
		return false;
	}

	/**
	 * Checks that the specified GitLab group exists also in the ldap tree
	 */
	public static boolean validateLdapGroupExistence(GitlabGroup gitlabGroup, LdapTree ldapTree) {
		return ldapTree.getGroups().contains(new LdapGroup(gitlabGroup.getName()));
	}

	/**
	 * Valide l'existence du groupe GitlabContext à partir d'un groupe ldap.
	 *
	 * @param ldapGroup Le groupe ldap.
	 * @param api       L'API GitlabContext.
	 * @return Vrai si le groupe existe, faux sinon.
	 */
	public static boolean validateGitlabGroupExistence(LdapGroup ldapGroup, GitlabAPI api) {
		try {
			api.getGroup(ldapGroup.getName());
			LOGGER.debug("Le groupe ldap [{}] existe dans GitLab", ldapGroup.getName());
			return true;
		} catch (IOException e) {
			LOGGER.debug("Le groupe ldap [{}] n'existe pas dans GitLab", ldapGroup.getName());
		}
		return false;
	}

	/**
	 * Vérifie que l'utilisateur existe bel et bien dans GitLab.
	 *
	 * @param user Un utilisateur ldap
	 * @param users Les utilisateurs GitlabContext.
	 * @return Vrai si l'utilisateur existe dans GitLab, faux sinon.
	 */
	public static boolean validateGitlabUserExistence(LdapUser user, List<GitlabUser> users) {
		long usersCount = users.stream()
				.filter(gitlabUser -> gitlabUser.getUsername().equals(user.getName()))
				.count();
		switch ((int) usersCount) {
			case 1:
				return true;
			case 0:
				return false;
			default:
				throw new IllegalStateException("Plus d'un utilisateur avec le nom " + user.getName() + " a été détecté");
		}
	}

	/**
	 * Détermine si l'utilisateur a des droits admin. Il y a plusieurs critères à vérifier.
	 *
	 * @param user     Le membre du groupe
	 * @param api      L'objet général GitLab
	 * @param ldapTree L'arbre représentant l'arborescence ldap.
	 * @return Vrai si l'utilisateur est admin, faux sinon.
	 */
	public static boolean isGitlabUserAdmin(GitlabUser user, GitlabAPI api, LdapTree ldapTree) {
		try {
			// S'agit-il de "moi" ?
			boolean isTechnicalAccount = user.getUsername().equals(api.getUser().getUsername());
			boolean isTrivialAdmin = user.isAdmin();
			// Ne serait-il pas par hasard dans le groupe ldap admin ?
			boolean isLdapAdmin = ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(user.getUsername());
			return isLdapAdmin || isTechnicalAccount || isTrivialAdmin;
		} catch (IOException e) {
			LOGGER.error("Erreur pendant l'évaluation des privilèges de l'utilisateur [{}]", user.getUsername());
		}
		return false;
	}

	public static Map<String, GitlabUser> getAllGitlabUsers(GitlabAPI api) {
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			api.getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));
			return allUsers;
		} catch (IOException e) {
			LOGGER.error("Impossible de récupérer tous les utilisateurs. L'erreur était : {}", e);
		}
		return new HashMap<>();
	}

	public static boolean isGitlabUserMemberOfGroup(List<GitlabGroupMember> members, String user){
		return members.stream()
				.filter(member -> member.getUsername().equals(user))
				.count() == 1;
	}

	public static GitlabUser getGitlabUser(GitlabAPI api, String username) {
			return getAllGitlabUsers(api).get(username);
	}

}
