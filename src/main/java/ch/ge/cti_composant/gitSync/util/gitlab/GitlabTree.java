package ch.ge.cti_composant.gitSync.util.gitlab;

import ch.ge.cti_composant.gitSync.util.AnnuaireTree;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
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
public class GitlabTree implements AnnuaireTree {
	Logger log = Logger.getLogger(GitlabTree.class.getName());
	String apiToken;

	private Map<GitlabGroup, Map<String, GitlabUser>> gitlabTree;

	public GitlabTree(String apiToken) throws IOException {
		gitlabTree = new HashMap<>();
		this.apiToken = apiToken;
		build();
	}

	/**
	 * Construction de l'arbre GitLab
	 * @throws IOException Erreur
	 */
	public void build() throws IOException {
		GitlabAPI api = GitlabAPI.connect(MiscConstants.GITLAB_BASE_URL_API, apiToken);
		api.getGroups().forEach(gitlabGroup -> {
			gitlabTree.put(gitlabGroup, new HashMap<>());
			try {
				api.getGroupMembers(gitlabGroup).forEach(user -> gitlabTree.get(gitlabGroup).put(user.getUsername(), user));
			} catch (IOException e){
				log.error("Une erreur s'est produite lors de la synchronisation du groupe " + gitlabGroup.getName() + " : " + e);
			}
		});
	}

	public List<GitlabGroup> getGroups(){
		return new ArrayList<>(this.gitlabTree.keySet());
	}

	public Map<String, GitlabUser> getUsers(GitlabGroup group){
		return new HashMap<>(gitlabTree.getOrDefault(group, new HashMap<>()));
	}

	public Map<String, GitlabUser> getUsers(){
		HashMap<String, GitlabUser> output = new HashMap<>();
		gitlabTree.forEach((group, users) -> {
			output.putAll(new HashMap<>(users));
		});
		return output;
	}
}
