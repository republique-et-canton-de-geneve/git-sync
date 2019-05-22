/*
 * gitSync
 *
 * Copyright (C) 2017-2019 République et canton de Genève
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ge.cti_composant.gitSync.util.ldap.gina;

import ch.ge.cti_composant.gitSync.GitSync;
import ch.ge.cti_composant.gitSync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeBuilder;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeSupport;
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser;
import gina.api.GinaApiLdapBaseAble;
import gina.impl.GinaLdapFactory;
import gina.impl.util.GinaLdapConfiguration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * An {@link LdapTree} that obtains its data (LDAP groups and LDAP users) from the Etat de Geneve's
 * LDAP server named Gina.
 * <p>
 * In order to retrieve LDAP groups and users from another LDAP server than GINA, in method {@link GitSync#setupLdap()}
 * you should replace the usage of this class with the usage of your own implementation of {@link LdapTreeBuilder}.
 * </p>
 */
public class GinaLdapTreeBuilder implements LdapTreeBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(GinaLdapTreeBuilder.class);

    /**
     * Names of the LDAP attributes to be retrieved from the LDAP server.
     * We are only interested in attribute "cn".
     */
    private static final String[] ATTRIBUTES = {"cn"};

	@Override
	public LdapTree createTree() {
		GinaApiLdapBaseAble app = null;
		LdapTree ldapTree;

		// create a search object on the Gina ldap server
		int timeout = Integer.parseInt(GitSync.getProperty("gina-ldap-client.ldap-timeout"));
		GinaLdapConfiguration conf = new GinaLdapConfiguration(
		        GitSync.getProperty("gina-ldap-client.ldap-server-url"),
				GitSync.getProperty("gina-ldap-client.ldap-base-dn"),
				GitSync.getProperty("gina-ldap-client.ldap-user"),
				GitSync.getProperty("gina-ldap-client.ldap-password"),
				GinaLdapConfiguration.Type.APPLICATION,
				timeout,
				timeout);
		try {
			// log on to the Gina LDAP server
			app = GinaLdapFactory.getInstance(conf);

			// initializations
			final GinaApiLdapBaseAble app2 = app;  // copy of reference, required by the compiler
			Map<LdapGroup, Map<String, LdapUser>> tree = new TreeMap<>(Comparator.comparing(LdapGroup::getName));

			// get the LDAP groups
			app.getAppRoles("GESTREPO").forEach(role -> tree.put(new LdapGroup(role), new TreeMap<>()));

			// get the LDAP users
			tree.forEach((ldapGroup, ldapUsers) -> {
				LOGGER.info("Retrieving the users of LDAP group [{}]", ldapGroup.getName());
				try {
					app2.getUsers("GESTREPO", ldapGroup.getName(), ATTRIBUTES)
							.forEach(user -> {
								if (user.containsKey("cn")) {
									LOGGER.info("\t{}", user.get("cn"));
									ldapUsers.put(user.get("cn"), new LdapUser(new HashMap<>(user)));
								}
							});
				} catch (RemoteException e) {
					LOGGER.error("Unable to retrieve the users from the LDAP server", e);
				}
			});
	        ldapTree = new LdapTreeSupport(tree);
		} catch (Exception e) {
			LOGGER.error("Exception caught while creating the LDAP tree", e);
			throw new GitSyncException(e);
		} finally {
			IOUtils.closeQuietly(app);
		}
        return ldapTree;
	}

}
