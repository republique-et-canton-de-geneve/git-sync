package ch.ge.cti_composant.gitSync;

import ch.ge.cti_composant.gitSync.missions.AddAuthorizedUsersToGroups;
import ch.ge.cti_composant.gitSync.missions.AddTechReadOnlyUsersToAllGroups;
import ch.ge.cti_composant.gitSync.missions.CheckMinimumUserCount;
import ch.ge.cti_composant.gitSync.missions.CleanGroupsFromUnauthorizedUsers;
import ch.ge.cti_composant.gitSync.missions.ImportGroupsFromLdap;
import ch.ge.cti_composant.gitSync.missions.PromoteAdminUsers;
import ch.ge.cti_composant.gitSync.missions.PropagateAdminUsersToAllGroups;
import ch.ge.cti_composant.gitSync.service.GitlabService;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeBuilder;
import ch.ge.cti_composant.gitSync.util.ldap.gina.GinaLdapTreeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Top-level class of the application: extracts the groups and users from the LDAP server and assigns them as groups
 * and users to the GitLab server.
 */
public class GitSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSync.class);
    
    private static final Properties props = new Properties();

    private LdapTree ldapTree = null;

    private Gitlab gitlab = null;

    /**
	 * Perfoms all operations.
	 */
	public void run(String path) {
		try {
			props.load(Files.newInputStream(Paths.get(path)));

			LOGGER.info("PHASE 1: Set up the in-memory LDAP tree");
			setupLdap();

			LOGGER.info("PHASE 2: Set up the in-memory GitLab tree");
			setupGitLab();

			LOGGER.info("PHASE 3: Apply the business rules");
			applyRules();

		} catch (Exception e) {
		    LOGGER.error("Exception caught while processing the LDAP/GitLab trees", e);
		}
	}

	/**
	 * Sets up the in-memory tree of LDAP groups and LDAP users.
	 */
    private void setupLdap() {
		// If you need to load the data from another LDAP server than Etat de Geneve's LDAP server, you must
		// replace the treeBuilder below with a custom one. See file README.md.
		LdapTreeBuilder treeBuilder = new GinaLdapTreeBuilder();
		ldapTree = treeBuilder.createTree();
	}

	/**
	 * Sets up the in-memory tree of GitLab groups and GitLab users.
	 */
    private void setupGitLab() {
		gitlab = new GitlabService().buildGitlabContext(
				props.getProperty("gitlab.hostname"),
				props.getProperty("gitlab.account.token"),
				ldapTree);
	}

    /**
	 * Performs the missions.
	 */
	private void applyRules() {
		// avoid to override GitLab groups and users with an empty configuration
		new CheckMinimumUserCount().start(ldapTree, gitlab);

		// create the groups in GitLab
		new ImportGroupsFromLdap().start(ldapTree, gitlab);

		// remove the non-authorized users
		new CleanGroupsFromUnauthorizedUsers().start(ldapTree, gitlab);

		// add the authorized users (new permissions)
		new AddAuthorizedUsersToGroups().start(ldapTree, gitlab);

		// add the Admins
		new PromoteAdminUsers().start(ldapTree, gitlab);

		// add the Admins to all groups
		new PropagateAdminUsersToAllGroups().start(ldapTree, gitlab);

		// add read-only permission to specific wide-access users on all groups
		new AddTechReadOnlyUsersToAllGroups().start(ldapTree, gitlab);
	}

	/**
	 * Returns the specified property, or null if not found.
	 */
	public static String getProperty(String name) {
		return props.getProperty(name);
	}

}
