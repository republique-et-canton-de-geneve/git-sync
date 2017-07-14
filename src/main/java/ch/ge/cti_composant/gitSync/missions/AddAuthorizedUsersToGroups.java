package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ajoute les utilisateurs autorisés au GitLab.
 */
public class AddAuthorizedUsersToGroups implements Mission {
	Logger log = Logger.getLogger(AddAuthorizedUsersToGroups.class.getName());

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : ajout des utilisateurs aux groupes autorisés.");
		try {
			Map<String, GitlabUser> allUsers = new HashMap<>();
			gitlab.getApi().getUsers().forEach(gitlabUser -> allUsers.put(gitlabUser.getUsername(), gitlabUser));

			for (GitlabGroup group : gitlab.getTree().getGroups()) {
				List<GitlabGroupMember> memberList = gitlab.getApi().getGroupMembers(group.getId());

				for (String username : ldapTree.getUsers(group.getName()).keySet()) {
					boolean isUserAlreadyMemberOfGroup = memberList.stream()
							.filter(member -> member.getUsername().equals(username)).count() == 1;

					if (allUsers.containsKey(username) && !isUserAlreadyMemberOfGroup) { // L'utilisateur existe dans Gitlab et n'a pas été ajouté au groupe.
						log.info("Ajout de l'utilisateur " + username + " au role " + group.getName() + "...");
						gitlab.getApi().addGroupMember(group, allUsers.get(username), GitlabAccessLevel.Master);

					} else if (allUsers.containsKey(username) && isUserAlreadyMemberOfGroup) { // L'utilisateur existe dans GitLab mais a déjà été ajouté au groupe.
						log.info("L'utilisateur " + username + " est déjà ajouté au groupe GitLab.");

					} else { // L'utilisateur n'existe pas.
						log.debug("L'utilisateur " + username + " n'existe pas dans GitLab.");
					}
				}
			}
		} catch (IOException e) {
			log.error("Impossible de récupérer la liste de tous les utilisateurs.");
		}
		log.info("Synchronisation terminée.");
	}
}
