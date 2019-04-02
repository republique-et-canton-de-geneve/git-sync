package ch.ge.cti_composant.gitSync.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import ch.ge.cti_composant.gitSync.util.LDAP.*;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.missions.AddAuthorizedUsersToGroups;
import ch.ge.cti_composant.gitSync.missions.AddTechReadOnlyUsersToAllGroups;
import ch.ge.cti_composant.gitSync.missions.CleanGroupsFromUnauthorizedUsers;
import ch.ge.cti_composant.gitSync.missions.ImportGroupsFromLDAP;
import ch.ge.cti_composant.gitSync.missions.PromoteAdminUsers;
import ch.ge.cti_composant.gitSync.missions.PropagateAdminUsersToAllGroups;

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
    	// if you need to load the data from another LDAP server than Etat de Geneve's LDAP server, replace the
		// treeFactory below with a custom one
    	LdapTreeFactory treeFactory = new GinaLdapTreeFactory();
		ldapTree = treeFactory.createTree();
	
		checkMimimumUserCount();

		/*
		gitlab = new GitlabContext(
				new Gitlab(
					props.getProperty("gitlab.account.token"),
					props.getProperty("gitlab.hostname"),
					ldapTree),
				props.getProperty("gitlab.hostname"),
				props.getProperty("gitlab.account.token"));
		*/

		gitlab = buildGitlabContext(
				props.getProperty("gitlab.hostname"),
				props.getProperty("gitlab.account.token"),
				ldapTree);
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
		    LOGGER.error("Erreur lors du chargement de l'arborescence LDAP/GitlabContext", e);
		}
		
		LOGGER.info("Job termine...");
	}

	public static String getProperty(String name) {
		return props.getProperty(name);
	}

    private void checkMimimumUserCount() throws IOException {
		// A hashset is used to insure a user is not added more than once
		Set<LDAPUser> users = new HashSet<>();
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

	/**
	 * Construction of the GitLab tree (groups and users), using the specified LDAP tree.
	 * @return the GitLab tree, <b>restricted to the elements that come from the LDAP server</b>.
	 */
	public Gitlab buildGitlabContext(String hostname, String apiToken, LDAPTree ldapTree) throws GitSyncException {
		// log on to GitLab
		GitlabAPI api = GitlabAPI.connect(hostname, apiToken);

		// retrieve the GitLab groups
		List<GitlabGroup> groups;
		try {
			groups = api.getGroups();
		} catch (IOException e) {
			LOGGER.error("Error caught while retrieving the GitLab groups", e);
			throw new GitSyncException(e);
		}

		// check and store the GitLab groups, including their users
		Map<GitlabGroup, Map<String, GitlabUser>> tree = new HashMap<>();
		groups.stream()
				// exclude the groups created by the user
				.filter(gitlabGroup -> MissionUtils.validateLDAPGroupExistence(gitlabGroup, ldapTree))
				// make sure the technical account owns the group
				.filter(gitlabGroup -> MissionUtils.validateGitlabGroupOwnership(gitlabGroup, api))
				.forEach(gitlabGroup -> {
					tree.put(gitlabGroup, new HashMap<>());
					try {
						api.getGroupMembers(gitlabGroup).forEach(user -> tree.get(gitlabGroup).put(user.getUsername(), user));
					} catch (IOException e) {
						LOGGER.error("Error caught while synchronizing group [{}] : {}", gitlabGroup.getName(), e);
					}
				});

		return new Gitlab(tree, hostname, apiToken);
	}

}
