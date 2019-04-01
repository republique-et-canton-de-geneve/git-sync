package ch.ge.cti_composant.gitSync.util.LDAP;

import java.util.List;
import java.util.Map;

/**
 * The tree of the users in the LDAP server.
 */
public interface LDAPTree {

	List<LDAPGroup> getGroups();

	Map<String, LDAPUser> getUsers(LDAPGroup group);

	Map<String, LDAPUser> getUsers(String group);

}
