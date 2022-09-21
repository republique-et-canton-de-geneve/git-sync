/*
 * gitsync
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
package ch.ge.cti_composant.gitsync.util.ldap.gina;

import ch.ge.cti_composant.gitsync.GitSync;
import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.exception.GitSyncException;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTreeBuilder;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTreeSupport;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;
import gina.api.GinaApiLdapBaseAble;
import gina.impl.GinaLdapAccess;
import gina.impl.util.GinaLdapConfiguration;
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
	 * Name of the Gina domain in the LDEP server.
	 */
	private static final String DOMAIN = "CTI";

	/**
	 * Name of the Gina application in the LDAP server.
	 */
	private static final String APPLICATION = "GESTREPO";

	/**
	 * Domain + Application.
	 */
	private static final String DOMAIN_APPLICATION = DOMAIN + "." + APPLICATION;

    /**
     * Names of the LDAP attributes to be retrieved from the LDAP server.
     * We are only interested in attribute "cn".
     */
    private static final String[] ATTRIBUTES = {"cn"};

	@Override
	public LdapTree createTree() {
		LdapTree ldapTree;

		// create a search object on the Gina LDAP server
		String ldapServer = GitSync.getProperty("gina-ldap-client.ldap-server-url");
		String ldapUser = GitSync.getProperty("gina-ldap-client.ldap-user");
		String ldapPassword = GitSync.getProperty("gina-ldap-client.ldap-password");
		int connectionTimeout = Integer.parseInt(GitSync.getProperty("gina-ldap-client.ldap-connection-timeout"));
		int readTimeout = Integer.parseInt(GitSync.getProperty("gina-ldap-client.ldap-read-timeout"));
		GinaLdapConfiguration ldapConf = new GinaLdapConfiguration(
				ldapServer, ldapUser, ldapPassword, DOMAIN, APPLICATION, readTimeout, connectionTimeout);
		try (GinaApiLdapBaseAble app = new GinaLdapAccess(ldapConf)) {
			// initializations
			Map<LdapGroup, Map<String, LdapUser>> tree = new TreeMap<>(Comparator.comparing(LdapGroup::getName));

			// get the LDAP groups
			app.getAppRoles(DOMAIN_APPLICATION)
				.stream()
				.filter(role -> MissionUtils.validateGroupnameCompliantStandardGroups(role) || role.equals(MissionUtils.getAdministratorGroup()) || role.equals(MissionUtils.getOwnerGroup()))
				.forEach(role -> tree.put(new LdapGroup(role), new TreeMap<>()));

			// get the LDAP users
			tree.forEach((ldapGroup, ldapUsers) -> {
				LOGGER.info("Retrieving the users of LDAP group [{}]", ldapGroup.getName());
				try {
				    	Comparator<Map<String, String>> userComparator = (user1, user2) -> { 
				    	    return user1.containsKey("cn") ? user1.get("cn").compareTo(user2.get("cn")) : 0;
				    	};

					app.getUsers(DOMAIN_APPLICATION, ldapGroup.getName(), ATTRIBUTES).stream()
					.sorted(userComparator)
							.forEach(user -> {
								if (user.containsKey("cn")) {
									LOGGER.info("\t{} is user of LDAP group [{}]", user.get("cn"), ldapGroup.getName());
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
		}
        return ldapTree;
	}

}
