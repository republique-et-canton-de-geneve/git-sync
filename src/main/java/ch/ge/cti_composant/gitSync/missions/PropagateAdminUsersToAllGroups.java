package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;
import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ajoute les admins à tous les groupes.
 */
public class PropagateAdminUsersToAllGroups implements Mission {
	Logger log = Logger.getLogger(PropagateAdminUsersToAllGroups.class.getName());

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
        						log.info("Ajout de " + admin.getUsername() + " à " + gitlabGroup.getName() + "...");
        						gitlab.getApi().addGroupMember(gitlabGroup, admin, GitlabAccessLevel.Master);
        					} else {
        						log.debug(admin.getUsername() + " est déjà membre du groupe " + gitlabGroup.getName());
        					}
        				}
			    	}
			}
		} catch (IOException e) {
			log.error("Une erreur est survenue lors de l'itération sur l'un des groupes. L'erreur était : " + e);
		}
	}
}
