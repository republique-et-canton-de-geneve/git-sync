package ch.ge.cti_composant.gitSync;

import ch.ge.cti_composant.gitSync.missions.AddAuthorizedUsersToGroups;
import ch.ge.cti_composant.gitSync.missions.AddTechReadOnlyUsersToAllGroups;
import ch.ge.cti_composant.gitSync.missions.CheckMinimumUserCount;
import ch.ge.cti_composant.gitSync.missions.CleanGroupsFromUnauthorizedUsers;
import ch.ge.cti_composant.gitSync.missions.ImportGroupsFromLDAP;
import ch.ge.cti_composant.gitSync.missions.PromoteAdminUsers;
import ch.ge.cti_composant.gitSync.missions.PropagateAdminUsersToAllGroups;
import ch.ge.cti_composant.gitSync.service.GitlabService;
import ch.ge.cti_composant.gitSync.util.LDAP_temp.GinaLdapTreeFactory;
import ch.ge.cti_composant.gitSync.util.LDAP_temp.LDAPTree;
import ch.ge.cti_composant.gitSync.util.LDAP_temp.LdapTreeFactory;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Extracts the roles and users from the LDAP_temp server and assigns them as groups and users to
 * the GitLab server.
 */
public class GitSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSync.class);
    
    private static final Properties props = new Properties();

    private LDAPTree ldapTree = null;

    private Gitlab gitlab = null;

    private void init() {
    	// set up the LDAP_temp tree of roles and LDAP_temp users.
		// If you need to load the data from another LDAP_temp server than Etat de Geneve's LDAP_temp server, you must
		// replace the treeFactory below with a custom one
		LdapTreeFactory treeFactory = new GinaLdapTreeFactory();
		ldapTree = treeFactory.createTree();

		// set up the GitLab tree of groups and GitLab users
		gitlab = new GitlabService().buildGitlabContext(
				props.getProperty("gitlab.hostname"),
				props.getProperty("gitlab.account.token"),
				ldapTree);
	}

    /**
	 * Performs the missions.
	 * @param path path to the property file
	 */
	public void run(String path) {
		try {
			props.load(Files.newInputStream(Paths.get(path)));
			init();

			// avoid to override GitLab groups and users with an empty configuration
			new CheckMinimumUserCount().start(ldapTree, gitlab);
			// create the groups in GitLab
			new ImportGroupsFromLDAP().start(ldapTree, gitlab);
			// remove the non-authorized users
			new CleanGroupsFromUnauthorizedUsers().start(ldapTree, gitlab);
			// add the authorized users (new permissions)
			new AddAuthorizedUsersToGroups().start(ldapTree, gitlab);
			// add the Admins
			new PromoteAdminUsers().start(ldapTree, gitlab);
			// add the Admins to all groups
			new PropagateAdminUsersToAllGroups().start(ldapTree, gitlab);
			// add read-only permission to user Fisheye on all groups
			new AddTechReadOnlyUsersToAllGroups().start(ldapTree, gitlab);
		} catch (Exception e) {
		    LOGGER.error("Exception caught while processing the LDAP_temp/GitLab trees", e);
		}
		
		LOGGER.info("End of execution");
	}

	public static String getProperty(String name) {
		return props.getProperty(name);
	}

}
