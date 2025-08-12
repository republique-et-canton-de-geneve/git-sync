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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple implementation of {@link LdapTree}.
 */
public class LdapTreeSupport implements LdapTree {

	/**
	 * The map containing the tree:
	 * <ul>
	 *     <li>a key is group</li>
	 *     <li>a value is in turn a map where a key is a user name and a value is a user</li>
	 * </ul>
	 */
	private final Map<LdapGroup, Map<String, LdapUser>> tree;

	public LdapTreeSupport(Map<LdapGroup, Map<String, LdapUser>> tree) {
		this.tree = tree;
	}

	public List<LdapGroup> getGroups() {
		return new ArrayList<>(tree.keySet()).stream()
				.sorted(Comparator.comparing(LdapGroup::getName))
				.toList();
	}

	public Map<String, LdapUser> getUsers(LdapGroup group) {
		return new TreeMap<>(tree.get(group));
	}

	public Map<String, LdapUser> getUsers(String groupName) {
		return StringUtils.isBlank(groupName) ? new HashMap<>() : getUsers(new LdapGroup(groupName));
	}

}
