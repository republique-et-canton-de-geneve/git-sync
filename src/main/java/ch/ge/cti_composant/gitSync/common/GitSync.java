package ch.ge.cti_composant.gitSync.common;

import ch.ge.cti_composant.gitSync.missions.*;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabTree;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;


/**
 * Main class that does the chit chat between all classes, basically
 */
public class GitSync {
	private Logger log = Logger.getLogger(GitSync.class.getName());
	// Chargement des propriétés
	Properties props = new Properties();
	private LDAPTree ldapTree;
	private Gitlab gitlab;

	private void init() throws IOException {
		ldapTree = new LDAPTree();
		gitlab = new Gitlab(
				new GitlabTree(props.getProperty("gitlab.account.token"), props.getProperty("gitlab.hostname"), ldapTree),
				props.getProperty("gitlab.hostname"), props.getProperty("gitlab.account.token")
		);
	}

	/**
	 * Exécute les missions.
	 * @param path L'adresse du fichier contenant les réglages
	 */
	public void run(String path) {
		try {
			props.load(Files.newInputStream(Paths.get(path)));
			init();

			// Importe les groupes LDAP vers GitLab
			new ImportGroupsFromLDAP().start(ldapTree, gitlab);
			// Supprime les utilisateurs non autorisés
			new CleanGroupsFromUnauthorizedUsers().start(ldapTree, gitlab);
			// Ajoute ceux qui le sont (nouvelles perms)
			new AddAuthorizedUsersToGroups().start(ldapTree, gitlab);
			// Ajoute les admins
			new PromoteAdminUsers().start(ldapTree, gitlab);
			// Ajoute les admins à tous les groupes
			new PropagateAdminUsersToAllGroups().start(ldapTree, gitlab);
		} catch (IOException e) {
			log.fatal("Erreur lors du chargement de l'arborescence LDAP/Gitlab. L'erreur était : " + e);
		}
		log.info("Jobs terminés...");
	}


}
