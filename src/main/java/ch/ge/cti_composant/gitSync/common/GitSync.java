package ch.ge.cti_composant.gitSync.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.missions.AddAuthorizedUsersToGroups;
import ch.ge.cti_composant.gitSync.missions.AddTechReadOnlyUsersToAllGroups;
import ch.ge.cti_composant.gitSync.missions.CleanGroupsFromUnauthorizedUsers;
import ch.ge.cti_composant.gitSync.missions.ImportGroupsFromLDAP;
import ch.ge.cti_composant.gitSync.missions.PromoteAdminUsers;
import ch.ge.cti_composant.gitSync.missions.PropagateAdminUsersToAllGroups;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.gitlab.GitlabTree;

/**
 * Main class that does the chit chat between all classes, basically
 */
public class GitSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSync.class);
    
    /**
     * The minimum user count. If there are less users that that count, we consider that there is a problem in
     * the VLDAP information retrieving process and the synchronisation is aborted. 
     */
    private static final int MIMIMUM_USER_COUNT = 10;

	/**
	 * Chargement des proprietes.
	 */
    private static final Properties props = new Properties();

    private LDAPTree ldapTree;

    private Gitlab gitlab;

    private void init() throws IOException {
	ldapTree = new LDAPTree();
	
	checkMimimumUserCount();

	gitlab = new Gitlab(new GitlabTree(props.getProperty("gitlab.account.token"),
		props.getProperty("gitlab.hostname"), ldapTree), props.getProperty("gitlab.hostname"),
		props.getProperty("gitlab.account.token"));
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
			// AJouter les droits de lecture au user Fisheye sur tous les groupes
			new AddTechReadOnlyUsersToAllGroups().start(ldapTree, gitlab);
		} catch (IOException e) {
		    LOGGER.error("Erreur lors du chargement de l'arborescence LDAP/Gitlab", e);
		}
		
		LOGGER.info("Job termine...");
	}

	public static String getProperty(String name) {
		return props.getProperty(name);
	}

    private void checkMimimumUserCount() throws IOException {
		// A hashset is used to insure a user is not added more than once
		Set<LDAPUser> users = new HashSet<LDAPUser>(512);
		for (LDAPGroup group : ldapTree.getGroups()) {
			for (LDAPUser user : ldapTree.getUsers(group).values()) {
				users.add(user);
			}
		}

		// Make sure the minimal count of users has been found
		LOGGER.info("Total number of groups = {}", ldapTree.getGroups().size());
		if (users.size() >= MIMIMUM_USER_COUNT) {
			LOGGER.info("Total number of users count = {}", users.size());
		} else {
			String message = "Total number of users count = " + users.size() + " < " + MIMIMUM_USER_COUNT+ " (mimum user count) -> we suspect a problem -> process aborted";
			LOGGER.error(message);
			throw new IOException(message);
		}
    }

}
