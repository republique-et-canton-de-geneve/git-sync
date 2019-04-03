package ch.ge.cti_composant.gitSync.util.LDAP_temp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of {@link LDAPTree}.
 */
public class LdapTreeSupport implements LDAPTree {

	/**
	 * The map containing the tree:
	 * <ul>
	 *     <li>a key is group</li>
	 *     <li>a value is in turn a map where a key is a user name and a value is a user</li>
	 * </ul>
	 */
	private final Map<LDAPGroup, Map<String, LDAPUser>> tree;

	public LdapTreeSupport(Map<LDAPGroup, Map<String, LDAPUser>> tree) {
		this.tree = tree;
	}

	public List<LDAPGroup> getGroups(){
		return new ArrayList<>(tree.keySet());
	}

	public Map<String, LDAPUser> getUsers(LDAPGroup group) {
//		return new HashMap<>(tree.getOrDefault(group, new HashMap<>()));
		return new HashMap<>(tree.get(group));
	}

	public Map<String, LDAPUser> getUsers(String group){
		return getUsers(new LDAPGroup(group));
	}

}
