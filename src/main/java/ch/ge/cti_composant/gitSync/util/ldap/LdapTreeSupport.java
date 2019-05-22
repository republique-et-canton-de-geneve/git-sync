package ch.ge.cti_composant.gitSync.util.ldap;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

	public List<LdapGroup> getGroups(){
		return new ArrayList<>(tree.keySet()).stream()
				.sorted(Comparator.comparing(LdapGroup::getName))
				.collect(Collectors.toList());
	}

	public Map<String, LdapUser> getUsers(LdapGroup group) {
		return new TreeMap<>(tree.get(group));
	}

	public Map<String, LdapUser> getUsers(String groupName) {
		return StringUtils.isBlank(groupName) ? new HashMap<>() : getUsers(new LdapGroup(groupName));
	}

	@Override
	public void addGroup(LdapGroup group) {

	}

}
