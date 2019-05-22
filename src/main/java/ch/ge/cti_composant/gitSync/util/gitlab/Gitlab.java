package ch.ge.cti_composant.gitSync.util.gitlab;

import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The GitLab context.
 * Consists mainly in the GitLab tree (groups and users), usually restricted to the elements that come from
 * the LDAP server.
 */
public class Gitlab {

	private GitlabAPIWrapper api;

	private Map<GitlabGroup, Map<String, GitlabUser>> tree;

	public Gitlab(Map<GitlabGroup, Map<String, GitlabUser>> tree, String url, String apiKey){
		this.tree = Objects.requireNonNull(tree);
		this.api = new GitlabAPIWrapper(GitlabAPI.connect(url, apiKey));
	}

	/**
	 * Returns the stub to the GitLab server.
	 */
	public GitlabAPIWrapper getApi() {
		return api;
	}

	/**
	 * Returns all GitLab groups, sorted by names.
	 */
	public List<GitlabGroup> getGroups() {
		return new ArrayList<>(tree.keySet()).stream()
				.sorted(Comparator.comparing(GitlabGroup::getName))
				.collect(Collectors.toList());
	}

	/**
	 * Returns all users of the specified GitLab group.
	 * @return a map where every key is a user name and every value is a GitLab user with that name
	 */
	public Map<String, GitlabUser> getUsers(GitlabGroup group) {
		return new HashMap<>(tree.getOrDefault(group, new HashMap<>()));
	}

	/**
	 * Returns all users.
	 * @return a map where every key is a user name and every value is a GitLab user with that name
	 */
	public Map<String, GitlabUser> getUsers() {
		HashMap<String, GitlabUser> output = new HashMap<>();
		tree.forEach((group, users) -> output.putAll(new HashMap<>(users)));
		return output;
	}

}
