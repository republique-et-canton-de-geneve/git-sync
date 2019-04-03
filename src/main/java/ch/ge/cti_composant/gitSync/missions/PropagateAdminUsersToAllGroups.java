package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
 * Adds the admin users to all groups.
 */
public class PropagateAdminUsersToAllGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropagateAdminUsersToAllGroups.class);

	@Override

	public void start(LdapTree ldapTree, Gitlab gitlab) {
		try {
			List<GitlabUser> admins = gitlab.getApi().getUsers().stream()
					.filter(GitlabUser::isAdmin)
					// remove the admins
					.filter(admin -> ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(admin.getUsername()))
					.collect(Collectors.toList());
			for (GitlabGroup gitlabGroup : gitlab.getGroups()) {
		    	// No op for users ***REMOVED*** ni ***REMOVED***
		    	if (!"***REMOVED***".equals(gitlabGroup.getName()) && !"***REMOVED***".equals(gitlabGroup.getName()) ) {
	   				List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
	   				for (GitlabUser admin : admins) {
	   					if (!MissionUtils.isGitlabUserMemberOfGroup(members, admin.getUsername())) {
							LOGGER.info("Adding user [{}] to group [{}]", admin.getUsername(), gitlabGroup.getName());
							gitlab.getApi().addGroupMember(gitlabGroup, admin, GitlabAccessLevel.Master);
						} else {
							LOGGER.info("User [{}] is already a member of group [{}]" , admin.getUsername(), gitlabGroup.getName());
						}
					}
		    	}
			}
		} catch (IOException e) {
			LOGGER.error("Exception caught while iterating on a group", e);
		}
	}

}
