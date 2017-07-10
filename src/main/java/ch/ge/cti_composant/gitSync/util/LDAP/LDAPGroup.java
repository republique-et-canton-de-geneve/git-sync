package ch.ge.cti_composant.gitSync.util.LDAP;

/**
 * Represents a LDAP Group.
 */
public class LDAPGroup {
	private String name;

	/**
	 * Override du constructeur p.d.p.d avec argument
	 * @param name Le nom du groupe
	 */
	public LDAPGroup(String name){
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass() == this.getClass()) {
			LDAPGroup group = (LDAPGroup) o;
			return group.name.equals(this.name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	public String getName(){
		return this.name;
	}

}
