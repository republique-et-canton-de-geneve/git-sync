package ch.ge.cti_composant.gitSync.util.LDAP_temp;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A user in the LDAP_temp server.
 */
public class LDAPUser {

	private HashMap<String, String> attributes;

	public LDAPUser(Map<String, String> attributes) {
		this.attributes = new HashMap<>(attributes);
		if (!this.attributes.containsKey("cn")) {
			throw new IllegalStateException("An LDAP_temp user needs to have a \"cn\" attribute. Actual attributes are: "
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
		/*
		Stream.of(
				new String[][] {
						{ "cn", "Jean Dupont" },
				        { "ou", "IT" }}).collect(Collectors.toMap(data -> data[0], data -> data[1]));
				        */
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
			LDAPUser user = (LDAPUser) o;
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
