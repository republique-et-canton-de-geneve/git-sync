package ch.ge.cti_composant.gitSync.util.ldap;

/**
 * A group in the LDAP server.
 * Will be matched to a group in the GitLab server.
 */
public class LdapGroup {

	private String name;

	public LdapGroup(String name){
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
			LdapGroup group = (LdapGroup) o;
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
