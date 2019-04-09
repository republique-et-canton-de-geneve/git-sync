package ch.ge.cti_composant.gitSync.util.ldap;

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
