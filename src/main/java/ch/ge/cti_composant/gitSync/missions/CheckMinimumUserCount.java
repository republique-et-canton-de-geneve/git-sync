package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CheckMinimumUserCount implements Mission {

	/**
	 * The minimum user count. If there are less users that that count, we consider that there is a problem in
	 * the VLDAP information retrieving process and the mapping is aborted.
	 */
	private static final int MIMIMUM_USER_COUNT = 10;

	private static final Logger LOGGER = LoggerFactory.getLogger(CheckMinimumUserCount.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Precondition: check that the LDAP tree is not empty or almost empty");
		// A hashset is used to insure a user is not added more than once

		Set<LdapUser> users = new HashSet<>();
		for (LdapGroup group : ldapTree.getGroups()) {
			for (LdapUser user : ldapTree.getUsers(group).values()) {
				users.add(user);
			}
		}

		// Make sure the minimal count of users has been found
		LOGGER.info("Total number of groups = {}", ldapTree.getGroups().size());
		if (users.size() >= MIMIMUM_USER_COUNT) {
			LOGGER.info("Total number of users = {}", users.size());
		} else {
			String message = "Total number of users = " + users.size() + " < " + MIMIMUM_USER_COUNT +
					" (minimum number of users) -> we suspect a problem -> process aborted";
			LOGGER.error(message);
			throw new GitSyncException(message);
		}
	}

}
