package ch.ge.cti_composant.gitSync.util.ldap.gina;

import ch.ge.cti_composant.gitSync.GitSync;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeFactory;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeSupport;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import gina.api.GinaApiLdapBaseAble;
import gina.impl.GinaLdapFactory;
import gina.impl.util.GinaLdapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An {@link LdapTree} that obtains its data (ldap groups and ldap users) from the Etat de Geneve's
 * ldap server named Gina.
 * <p>
 * In order to retrieve ldap groups and users from another ldap server than GINA, you should replace the usage
 * of this class with the usage of a custom implementation of {@link LdapTree}.
 * </p>
 */
public class GinaLdapTreeFactory implements LdapTreeFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(GinaLdapTreeFactory.class);

    /**
     * Names of the ldap attributes to be retrieved from the ldap server.
     * We are only interested in attribute "cn".
     */
    private static final String[] ATTRIBUTES = {"cn"};

	@Override
	public LdapTree createTree() {
		// create a search object on the Gina ldap server
		int timeout = Integer.parseInt(GitSync.getProperty("timeout-search-ldap"));
		GinaLdapConfiguration conf = new GinaLdapConfiguration(
		        GitSync.getProperty("ct-gina-ldap-client.LDAP_SERVER_URL"),
				GitSync.getProperty("ct-gina-ldap-client.LDAP_BASE_DN"),
				GitSync.getProperty("ct-gina-ldap-client.LDAP_USER"),
				GitSync.getProperty("ct-gina-ldap-client.LDAP_PASSWORD"),
				GinaLdapConfiguration.Type.APPLICATION,
				timeout,
				timeout);
		GinaApiLdapBaseAble app = GinaLdapFactory.getInstance(conf);

		// create and fill the tree
		Map<LdapGroup, Map<String, LdapUser>> tree = new HashMap<>();
		try {
			// get the groups
			app.getAppRoles("GESTREPO").forEach(role -> tree.put(new LdapGroup(role), new TreeMap<>()));

			// get the users
			tree.forEach((ldapGroup, ldapUsers) -> {
				LOGGER.info("Retrieving the users of LDAP group [{}]", ldapGroup.getName());
				try {
					app.getUsers("GESTREPO", ldapGroup.getName(), ATTRIBUTES)
							.forEach(user -> {
								if (user.containsKey("cn")) {
									LOGGER.info("\t{}", user.get("cn"));
									ldapUsers.put(user.get("cn"), new LdapUser(new HashMap<>(user)));
								}
							});
				} catch (RemoteException e) {
					LOGGER.error("Unable to logon to the ldap server", e);
				}
			});
		} catch (IOException e) {
			LOGGER.error("Unable to run the ldap search, because file distribution.properties could not be found", e);
			throw new GitSyncException("Exception caught while creating the ldap tree", e);
		}

	    return new LdapTreeSupport(tree);
	}

}
