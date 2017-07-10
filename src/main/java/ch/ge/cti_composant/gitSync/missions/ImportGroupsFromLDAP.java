package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.CreateGroupRequest;

import java.io.IOException;

public class ImportGroupsFromLDAP implements Mission {
	Logger log = Logger.getLogger(ImportGroupsFromLDAP.class.getName());

	/**
	 * Charge les rôles Gina, puis crée les groupes dans GitLab.
	 * @implNote Le/s groupe/s considérés comme admins n'auront pas de groupe créé.
	 */
	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		log.info("Synchronisation : Groupes LDAP à groupes GitLab");
		ldapTree.getGroups().forEach(ldapGroup -> {
			if (isLDAPGroupAdmin(ldapGroup.getName())){
				log.info("Groupe administrateur détecté, pas de création de groupe.");
				// NOTE les utilisateurs administrateurs sont dans une mission à part. Donc rien n'est fait ici...
			} else {
				try	{
					gitlab.getApi().getGroup(ldapGroup.getName());
					log.debug("Le groupe " + ldapGroup.getName() + " existe. Rien ne sera fait.");
				} catch (IOException e) {
					log.info("Groupe inexistant sur GitLab détecté : " + ldapGroup.getName() + " ! Création en cours...");
					// Création du groupe
					CreateGroupRequest createGroupRequest = new CreateGroupRequest(ldapGroup.getName(), ldapGroup.getName());
					createGroupRequest.setVisibility("private");
					try	{
						gitlab.getApi().createGroup(createGroupRequest, gitlab.getApi().getUser());
					} catch (IOException e2){
						log.fatal("Impossible de créer le groupe " + ldapGroup.getName() + " : " + e2);
					}
				}

			}
		});
		log.info("Synchronisation terminée.");
	}

	private static boolean isLDAPGroupAdmin(String role) {
		return role.equals(MiscConstants.ADMIN_LDAP_GROUP);
	}
}
