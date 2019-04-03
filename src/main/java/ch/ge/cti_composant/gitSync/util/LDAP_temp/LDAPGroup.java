package ch.ge.cti_composant.gitSync.util.LDAP_temp;

/**
 * A group in the LDAP_temp server.
 */
public class LDAPGroup {

	private String name;

	public LDAPGroup(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		} else if (o.getClass() == getClass()) {
			LDAPGroup group = (LDAPGroup) o;
			return group.name.equals(name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

}
