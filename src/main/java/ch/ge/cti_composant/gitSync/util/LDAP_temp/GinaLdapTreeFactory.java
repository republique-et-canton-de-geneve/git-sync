package ch.ge.cti_composant.gitSync.util.LDAP_temp;

import ch.ge.cti_composant.gitSync.GitSync;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import gina.api.GinaApiLdapBaseAble;
import gina.impl.GinaLdapFactory;
import gina.impl.util.GinaLdapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link LDAPTree} that obtains its data (LDAP_temp groups and LDAP_temp users) from the Etat de Geneve's
 * LDAP_temp server named Gina.
 * <p>
 * In order to retrieve LDAP_temp groups and users from another LDAP_temp server than GINA, you should replace the usage
 * of this class with the usage of a custom implementation of {@link LDAPTree}.
 * </p>
 */
public class GinaLdapTreeFactory implements LdapTreeFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(GinaLdapTreeFactory.class);

    /**
     * Names of the LDAP_temp attributes to be retrieved from the LDAP_temp server.
     * We are only interested in attribute "cn".
     */
    private static final String[] ATTRIBUTES = {"cn"};

	@Override
	public LDAPTree createTree() throws GitSyncException {
		// create a search object on the Gina LDAP_temp server
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
		Map<LDAPGroup, Map<String, LDAPUser>> tree = new HashMap<>();
		try {
			// get the groups
			app.getAppRoles("GESTREPO").forEach(role -> tree.put(new LDAPGroup(role), new HashMap<>()));

			// get the users
			tree.forEach((ldapGroup, ldapUsers) -> {
				LOGGER.info("Retrieving the users for LDAP_temp group [{}]", ldapGroup.getName());
				try {
					app.getUsers("GESTREPO", ldapGroup.getName(), ATTRIBUTES)
							.forEach(user -> {
								if (user.containsKey("cn")) {
									LOGGER.info("\t{}", user.get("cn"));
									ldapUsers.put(user.get("cn"), new LDAPUser(new HashMap<>(user)));
								}
							});
				} catch (RemoteException e) {
					LOGGER.error("Unable to logon to the LDAP_temp server", e);
				}
			});
		} catch (IOException e) {
			LOGGER.error("Unable to run the LDAP_temp search, because file distribution.properties could not be found", e);
			throw new GitSyncException("Exception caught while creating the LDAP_temp tree", e);
		}

	    return new LdapTreeSupport(tree);
	}

}
