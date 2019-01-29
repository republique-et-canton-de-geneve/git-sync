package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * Ajoute les admins Ã  tous les groupes.
 */
public class PropagateAdminUsersToAllGroups implements Mission {
	private static final Logger LOGGER = LoggerFactory.getLogger(PropagateAdminUsersToAllGroups.class);

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		try {
			List<GitlabUser> admins = gitlab.getApi().getUsers().stream()
					.filter(GitlabUser::isAdmin)
					// Filtre supprimant les admins
					.filter(admin -> ldapTree.getUsers(MiscConstants.ADMIN_LDAP_GROUP).containsKey(admin.getUsername()))
					.collect(Collectors.toList());
			for (GitlabGroup gitlabGroup : gitlab.getTree().getGroups()) {
			    	
			    	//
			    	// Ne pas le faire pour ***REMOVED*** ni ***REMOVED***
			    	//
			    
			    	if (!"***REMOVED***".equals(gitlabGroup.getName()) && !"***REMOVED***".equals(gitlabGroup.getName()) ) {
			    	    
        				List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
        				for (GitlabUser admin : admins) {
        					if (!MissionUtils.isGitlabUserMemberOfGroup(members, admin.getUsername())) {
        						LOGGER.info("Ajout de " + admin.getUsername() + " à " + gitlabGroup.getName());
        						gitlab.getApi().addGroupMember(gitlabGroup, admin, GitlabAccessLevel.Master);
        					} else {
        						LOGGER.info(admin.getUsername() + " est déjà  membre du groupe " + gitlabGroup.getName());
        					}
        				}
			    	}
			}
		} catch (IOException e) {
			LOGGER.error("Une erreur est survenue lors de l'itÃ©ration sur l'un des groupes : " + e);
		}
	}
}
