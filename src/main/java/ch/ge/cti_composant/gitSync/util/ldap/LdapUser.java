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
package ch.ge.cti_composant.gitSync.util.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A user in the LDAP server.
 */
public class LdapUser {

	private HashMap<String, String> attributes;

	public LdapUser(Map<String, String> attributes) {
		this.attributes = new HashMap<>(attributes);
		if (!this.attributes.containsKey("cn")) {
			throw new IllegalStateException("An LDAP user needs to have a \"cn\" attribute. Actual attributes are: "
					+ attributes);
		}
	}

	/**
	 * Gets an attribute from the user.
	 *
	 * @param key attribute key
	 * @return value of the attribute
	 * @throws NullPointerException if the attribute does not exist
	 */
	public String getAttribute(String key) {
		return Objects.requireNonNull(attributes.get(key), "No such attribute '" + key + "'");
	}

	/**
	 * Returns the name of the user.
	 */
	public String getName() {
		return getAttribute("cn");
	}

	@Override
	public int hashCode() {
		return attributes.get("cn").hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o.getClass() == getClass()) {
			LdapUser user = (LdapUser) o;
			return attributes.get("cn").equals(user.attributes.get("cn"));
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return getName();
	}

}
