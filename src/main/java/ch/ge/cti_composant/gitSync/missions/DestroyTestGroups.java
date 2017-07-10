package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;

import java.io.IOException;
import java.util.List;

/**
 * Détruit les groupes GitLab du moment que le seul utilisateur soit nous ET que nous sommes le propriétaire.
 */
public class DestroyTestGroups implements Mission {
	Logger log = Logger.getLogger(DestroyTestGroups.class.getName());

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("[DEBUG] Synchronisation : suppression des groupes vides dont je suis le propriétaire. Ne PAS m'utiliser en prod.");
		ldapTree.getGroups().stream().filter(group -> !group.getName().equals(MiscConstants.ADMIN_LDAP_GROUP))
				.forEach(ldapGroup -> {
					try {
						GitlabGroup groupToRemove = gitlab.getApi().getGroup(ldapGroup.getName());
						List<GitlabGroupMember> memberList = gitlab.getApi().getGroupMembers(groupToRemove.getId());
						if (memberList.size() == 1) {
							for (GitlabGroupMember member : memberList) {
								if (member.getAccessLevel() == GitlabAccessLevel.Owner && member.getId().equals(gitlab.getApi().getUser().getId())) {
									log.warn("Suppression du groupe " + ldapGroup.getName() + "...");
									gitlab.getApi().deleteGroup(groupToRemove.getId());
								} else {
									log.info("Le groupe " + ldapGroup.getName() + " n'a pas les prérequis de suppression.");
								}
							}
						} else {
							log.info("Le groupe " + ldapGroup.getName() + " a plus d'un membre. Ignoré.");
						}
					} catch (IOException e) {
						log.error("Impossible de supprimer le groupe " + ldapGroup.getName() + ". L'erreur était : " + e);
					}
				});
		log.info("Synchronisation terminée.");
	}
}
