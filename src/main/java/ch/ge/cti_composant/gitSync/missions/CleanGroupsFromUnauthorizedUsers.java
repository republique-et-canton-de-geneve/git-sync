package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.GitlabGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;

/**
 * Removes the permissions in excess on GitLab.
 * <br/>
 * Admin users are ignored. They can be assigned to any type of group or project.
 */
public class CleanGroupsFromUnauthorizedUsers implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImportGroupsFromLdap.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Mapping: mapping the users with the LDAP server users");

		// for every group...
		gitlab.getGroups().stream()
				.sorted(Comparator.comparing(GitlabGroup::getName))
				.forEach(gitlabGroup -> {
					LOGGER.info("    Mapping group [{}]", gitlabGroup.getName());
					handleGroup(gitlabGroup, ldapTree, gitlab);
			});

		LOGGER.info("Mapping completed");
	}

	private void handleGroup(GitlabGroup gitlabGroup, LdapTree ldapTree, Gitlab gitlab) {
		LdapGroup ldapGroup = new LdapGroup(gitlabGroup.getName());

		// for every user...
		try {
			gitlab.getApi().getGroupMembers(gitlabGroup.getId()).stream()
					.filter(gitlabGroupMember -> !MissionUtils.isGitlabUserAdmin(gitlabGroupMember, gitlab.getApi(), ldapTree))
					.filter(member -> !ldapTree.getUsers(ldapGroup.getName()).containsKey(member.getUsername()))
					.forEach(member -> {
						if ((!MiscConstants.FISHEYE_USERNAME.equals(member.getUsername()))
							&& (!MiscConstants.MWFL_USERNAME.equals(member.getUsername()))) {
					    	LOGGER.info("        User [{}] does not belong or no longer belongs to group [{}]",
									member.getUsername(), gitlabGroup.getName());
    						try {
    							gitlab.getApi().deleteGroupMember(gitlabGroup, member);
    						} catch (IOException e) {
    							LOGGER.error("Exception caught while removing user [" + member.getUsername() +
										"] from group [" + gitlabGroup.getName() + "]", e);
    						}
					    }
					});
		} catch (IOException e) {
			LOGGER.error("Exception caught while processing group [{}]", gitlabGroup.getName(), e);
		}
	}

}
