package ch.ge.cti_composant.gitSync.common;

import ch.ge.cti_composant.gitSync.missions.DestroyTestGroups;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabTree;
import org.apache.log4j.Logger;

import java.io.IOException;
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
		gitlab = new Gitlab(new GitlabTree(props.getProperty("gitlab.account.token")), MiscConstants.GITLAB_BASE_URL_API, props.getProperty("gitlab.account.token"));
	}

	public void run() {
		try {
			props.load(GitSync.class.getResourceAsStream("/distribution.properties"));
			init();

			// Importe les groupes LDAP vers GitLab
			//new ImportGroupsFromLDAP().start(ldapTree, gitlab);
			// Synchronise les utilisateurs
			//new SyncUsersWithLDAP().start(ldapTree, gitlab);
			// Supprime les groupes de test
			new DestroyTestGroups().start(ldapTree, gitlab);
		} catch (IOException e) {
			log.fatal("Erreur lors du chargement de l'arborescence LDAP/Gitlab. L'erreur était : " + e);
		}
		log.info("Jobs terminés...");
	}


}
