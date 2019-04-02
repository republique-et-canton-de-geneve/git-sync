package ch.ge.cti_composant.gitSync.util.gitlab;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The GitLab context.
 * Consists mainly in the GitLab tree (groups and users), usually restricted to the elements that come from
 * the LDAP server.
 */
public class Gitlab {

	private GitlabAPI api;

	private Map<GitlabGroup, Map<String, GitlabUser>> tree;

	public Gitlab(Map<GitlabGroup, Map<String, GitlabUser>> tree, String url, String apiKey){
		this.tree = Objects.requireNonNull(tree);
		this.api = GitlabAPI.connect(url, apiKey);
	}

	public GitlabAPI getApi() {
		return api;
	}

	/**
	 * Return all GitLab groups.
	 */
	public List<GitlabGroup> getGroups() {
		return new ArrayList<>(tree.keySet());
	}

	/**
	 * Return all users of the specified GitLab group.
	 */
	public Map<String, GitlabUser> getUsers(GitlabGroup group) {
		return new HashMap<>(tree.getOrDefault(group, new HashMap<>()));
	}

	/**
	 * Return all users.
	 */
	public Map<String, GitlabUser> getUsers() {
		HashMap<String, GitlabUser> output = new HashMap<>();
		tree.forEach((group, users) -> output.putAll(new HashMap<>(users)));
		return output;
	}

}
