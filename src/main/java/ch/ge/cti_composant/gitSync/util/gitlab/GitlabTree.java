package ch.ge.cti_composant.gitSync.util.gitlab;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import org.apache.log4j.Logger;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente l'arborescence GitLab restreinte aux éléments venant du LDAP.
 */
public class GitlabTree {
	Logger log = Logger.getLogger(GitlabTree.class.getName());
	String apiToken;
	String hostname;

	private Map<GitlabGroup, Map<String, GitlabUser>> gitlabTree;

	public GitlabTree(String apiToken, String hostname, LDAPTree ldapTree) throws IOException {
		gitlabTree = new HashMap<>();
		this.apiToken = apiToken;
		this.hostname = hostname;
		build(ldapTree);
	}

	/**
	 * Construction de l'arbre GitLab
	 *
	 * @throws IOException Erreur
	 */
	public void build(LDAPTree ldapTree) throws IOException {
		GitlabAPI api = GitlabAPI.connect(hostname, apiToken);
		api.getGroups().stream()
				// De cette façon, on évite de prendre des groupes créés par l'utilisateur
				.filter(gitlabGroup -> MissionUtils.validateLDAPGroupExistence(gitlabGroup, ldapTree))
				// ...et on s'assure que c'est bien le compte technique qui a la main sur le groupe
				.filter(gitlabGroup -> MissionUtils.validateGitlabGroupOwnership(gitlabGroup, api))
				.forEach(gitlabGroup -> {
					gitlabTree.put(gitlabGroup, new HashMap<>());
					try {
						api.getGroupMembers(gitlabGroup).forEach(user -> gitlabTree.get(gitlabGroup).put(user.getUsername(), user));
					} catch (IOException e) {
						log.error("Une erreur s'est produite lors de la synchronisation du groupe " + gitlabGroup.getName() + " : " + e);
					}
				});
	}

	public List<GitlabGroup> getGroups() {
		return new ArrayList<>(this.gitlabTree.keySet());
	}

	public Map<String, GitlabUser> getUsers(GitlabGroup group) {
		return new HashMap<>(gitlabTree.getOrDefault(group, new HashMap<>()));
	}

	public Map<String, GitlabUser> getUsers() {
		HashMap<String, GitlabUser> output = new HashMap<>();
		gitlabTree.forEach((group, users) -> {
			output.putAll(new HashMap<>(users));
		});
		return output;
	}
}
