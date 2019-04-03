package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;

/**
 * Ajoute les utilisateurs autorisés au GitLab.
 */
public class AddAuthorizedUsersToGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(AddAuthorizedUsersToGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Synchronisation : ajout des utilisateurs aux groupes autorises");
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			for (GitlabGroup group : gitlab.getGroups()) {
				List<GitlabGroupMember> memberList = gitlab.getApi().getGroupMembers(group.getId());
				LOGGER.info("Gestion des utilisateurs du groupe [{}]...", group.getName());

				for (String username : ldapTree.getUsers(group.getName()).keySet()) {
					boolean isUserAlreadyMemberOfGroup = memberList.stream()
							.filter(member -> member.getUsername().equals(username))
							.count() == 1;

					if (allUsers.containsKey(username) && !isUserAlreadyMemberOfGroup) {
						// L'utilisateur existe dans GitlabContext et n'a pas été ajouté au groupe.
						LOGGER.info("Ajout de l'utilisateur [{}] au groupe [{}]", username, group.getName());
						gitlab.getApi().addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Master);
					} else if (allUsers.containsKey(username) && isUserAlreadyMemberOfGroup) {
						// L'utilisateur existe dans GitLab mais a déjà été ajouté au groupe.
						LOGGER.info("L'utilisateur [{}] est deja dans le groupe GitLab [{}]", username, group.getName());
					} else {
						// L'utilisateur n'existe pas.
						LOGGER.info("L'utilisateur [{}] n'existe pas dans GitLab", username);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Impossible de recuperer la liste de tous les utilisateurs");
		}
		LOGGER.info("Synchronisation terminee");
	}

}
