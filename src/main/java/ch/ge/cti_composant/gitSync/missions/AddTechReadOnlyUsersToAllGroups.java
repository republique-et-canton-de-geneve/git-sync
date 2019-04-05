package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.List;

import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;

/**
 * Adds the Admin users to all groups.
 */
public class AddTechReadOnlyUsersToAllGroups implements Mission {
    	
	private static final Logger LOGGER = LoggerFactory.getLogger(AddTechReadOnlyUsersToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
	    addUser(gitlab, MiscConstants.FISHEYE_USERNAME);
	    addUser(gitlab, MiscConstants.MWFL_USERNAME);
	}
	
	private void addUser(Gitlab gitlab, String username) {
		LOGGER.info("Adding administrator users to all groups with Reporter permissions");
		try {
			GitlabUser user = MissionUtils.getGitlabUser(gitlab.getApi(), username);

			if (user != null) {
				for (GitlabGroup gitlabGroup : gitlab.getGroups()) {
					// no op for ***REMOVED*** and ***REMOVED***
					if (!"***REMOVED***".equals(gitlabGroup.getName()) && !"***REMOVED***".equals(gitlabGroup.getName()) ) {
						List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
						if (!MissionUtils.isGitlabUserMemberOfGroup(members, username)) {
							LOGGER.info("    Adding user [{}] to group [{}]", username, gitlabGroup.getName());
							gitlab.getApi().addGroupMember(gitlabGroup, user, GitlabAccessLevel.Reporter);
						} else {
							LOGGER.info("    User [{}] is already a member of group [{}]", username, gitlabGroup.getName());
						}
					}
				}
			} else {
				LOGGER.info("    User [{}] is not a GitLab user", username);
			}
		} catch (IOException e) {
			LOGGER.error("Exception caught while iterating on a group", e);
		}
		LOGGER.info("Adding administrator users completed");
	}

}
