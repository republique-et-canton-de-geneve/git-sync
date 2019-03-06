package ch.ge.cti_composant.gitSync.util.gitlab;

import org.gitlab.api.GitlabAPI;

import java.util.Objects;

/**
 * Représente le contexte Gitlab total
 */
public class Gitlab {

	private GitlabTree tree;

	private GitlabAPI api;

	public Gitlab(GitlabTree tree, String url, String apiKey){
		this.tree = Objects.requireNonNull(tree);
		this.api = GitlabAPI.connect(url, apiKey);
	}

	public GitlabTree getTree() {
		return tree;
	}

	public GitlabAPI getApi() {
		return api;
	}

}
