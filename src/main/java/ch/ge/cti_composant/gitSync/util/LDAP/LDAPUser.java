package ch.ge.cti_composant.gitSync.util.LDAP;

import java.util.HashMap;
import java.util.Objects;

/**
 * Represents a user in LDAP.
 */
public class LDAPUser {

	private HashMap<String, String> attributes;

	public LDAPUser(HashMap<String, String> attributes) {
		this.attributes = new HashMap<>(attributes);
		if (!this.attributes.containsKey("cn")) {
			throw new IllegalStateException("Chaque utilisateur LDAP a besoin d'un CN. - {}" + attributes);
		}
	}

	/**
	 * Gets an attribute from the user.
	 *
	 * @param key The attribute key.
	 * @return String The value of the attribute.
	 * @throws NullPointerException If the attribute doesnt exist.
	 */
	public String getAttribute(String key) {
		return Objects.requireNonNull(attributes.get(key), "Inexistant attribute.");
	}

	/**
	 * Raccourci : permet de récupérer le nom de l'utilisateur.
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
		if (o.getClass() == getClass()) {
			LDAPUser user = (LDAPUser) o;
			return attributes.get("cn").equals(user.attributes.get("cn"));
		} else {
			return false;
		}
	}

}
