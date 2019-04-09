package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Adds the admin users to all groups.
 */
public class PropagateAdminUsersToAllGroups implements Mission {

	private static final Logger LOGGER = LoggerFactory.getLogger(PropagateAdminUsersToAllGroups.class);

	@Override
	public void start(LdapTree ldapTree, Gitlab gitlab) {
		LOGGER.info("Propagating admin users to all groups");

		List<GitlabUser> admins = gitlab.apiGetUsers().stream()
				.filter(GitlabUser::isAdmin)
				.filter(admin -> ldapTree.getUsers(MissionUtils.getAdministratorGroup()).containsKey(admin.getUsername()))
				.sorted(Comparator.comparing(GitlabUser::getUsername))
				.collect(Collectors.toList());
		gitlab.getGroups().stream()
				// no op for the black-listed groups
				.filter(group -> !MissionUtils.getBlackListedGroups().contains(group.getName()))
				.forEach(group -> {
						List<GitlabGroupMember> members = gitlab.apiGetGroupMembers(group.getId());
						admins.stream()
								.forEach(admin -> {
									if (!MissionUtils.isGitlabUserMemberOfGroup(members, admin.getUsername())) {
										LOGGER.info("    Adding user [{}] to group [{}]",
												admin.getUsername(), group.getName());
										gitlab.apiAddGroupMember(group, admin, GitlabAccessLevel.Master);
									} else {
										LOGGER.info("    User [{}] is already a member of group [{}]" ,
												admin.getUsername(), group.getName());
									}
								});
				});

		LOGGER.info("Propagating completed");
	}

}
