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
package ch.ge.cti_composant.gitsync.util.ldap;

import java.util.List;
import java.util.Map;

/**
 * The tree of the users in the LDAP server.
 */
public interface LdapTree {

	List<LdapGroup> getGroups();

	Map<String, LdapUser> getUsers(LdapGroup group);

	/**
	 * Returns the users of the specified LDAP group.
	 * @param groupName LDAP group name. Can be blank
	 * @return a map of users, possibly empty
	 */
	Map<String, LdapUser> getUsers(String groupName);

}
