package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Removes the permissions in excess on GitLab.
 * <br/>
 * Admin users are ignored. They can be assigned to any type of group or project.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanGroupsFromUnauthorizedUsers.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: removing the user permissions in excess on GitLab");

		// for every group...
		gitlab.getGroups().stream()
				.sorted(Comparator.comparing(GitlabGroup::getName))
				.forEach(gitlabGroup -> {
					LOGGER.info("    Processing group [{}]", gitlabGroup.getName());
					handleGroup(gitlabGroup, ldapTree, gitlab);
			});

		LOGGER.info("Mapping completed");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LdapTree ldapTree, Gitlab gitlab) {
		LdapGroup ldapGroup = new LdapGroup(gitlabGroup.getName());

		// for every user...
		gitlab.apiGetGroupMembers(gitlabGroup.getId()).stream()
				.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
				.filter(member -> !MissionUtils.isGitlabUserAdmin(member, gitlab.getApi(), ldapTree))
				.filter(member -> !MissionUtils.getWideAccessUsers().contains(member.getUsername()))
				.forEach(member -> {
					LOGGER.info("        Removing user [{}] from group [{}]",
							member.getUsername(), gitlabGroup.getName());
					gitlab.apiDeleteGroupMember(gitlabGroup, member);
				});
	}

}
