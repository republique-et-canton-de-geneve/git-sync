package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.GitSync;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CheckMinimumUserCount implements Mission {

	/**
	 * Default value of property "minimum-user-count".
	 * It is used if no value for "minimum-user-count" is supplied in the configuration file.
	 */
	private static final int DEFAULT_MINIMUM_USER_COUNT = 5;

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckMinimumUserCount.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Precondition: check that the LDAP tree is not empty or almost empty");

		// A hashset is used to ensure a user is not added more than once
		Set<LdapUser> users = new HashSet<>();
		for (LdapGroup group : ldapTree.getGroups()) {
			users.addAll(ldapTree.getUsers(group).values());
		}

		// Make sure the minimal count of users has been found
		LOGGER.info("Total number of groups = {}", ldapTree.getGroups().size());
		int minimumUserCount = getMinimumUserCount();
		if (users.size() >= minimumUserCount) {
			LOGGER.info("Total number of users = {}", users.size());
		} else {
			String message = "Total number of users (" + users.size() + ") < minimum number of users ("
					+ minimumUserCount + ") -> we suspect a problem -> process aborted";
			LOGGER.error(message);
			throw new GitSyncException(message);
		}

		LOGGER.info("Precondition OK");
	}

	private int getMinimumUserCount() {
		String prop = GitSync.getProperty("minimum-user-count");
		return prop == null ? DEFAULT_MINIMUM_USER_COUNT : new Integer(prop);
	}

}
