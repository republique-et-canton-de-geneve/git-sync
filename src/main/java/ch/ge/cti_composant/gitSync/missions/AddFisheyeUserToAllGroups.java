package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.gitlab.api.models.GitlabAccessLevel;
import org.gitlab.api.models.GitlabGroup;
import org.gitlab.api.models.GitlabGroupMember;
import org.gitlab.api.models.GitlabUser;

import ch.ge.cti_composant.gitSync.util.MiscConstants;
import ch.ge.cti_composant.gitSync.util.MissionUtils;
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * Ajoute les admins à tous les groupes.
 */
public class AddFisheyeUserToAllGroups implements Mission {
    	
	Logger log = Logger.getLogger(AddFisheyeUserToAllGroups.class.getName());

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
		try {
		    	GitlabUser user = MissionUtils.getGitlabUser(gitlab.getApi(), MiscConstants.FISHEYE_USERNAME);
		    	
		    	if (user != null)
		    	{
		    	    for (GitlabGroup gitlabGroup : gitlab.getTree().getGroups()) {
    			    	
    			    	//
    			    	// Ne pas le faire pour ***REMOVED*** ni ***REMOVED***
    			    	//
    			    
    			    	if (!"***REMOVED***".equals(gitlabGroup.getName()) && !"***REMOVED***".equals(gitlabGroup.getName()) ) {
    			    	    
               				List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
               				
            				if (!MissionUtils.isGitlabUserMemberOfGroup(members, MiscConstants.FISHEYE_USERNAME)) {
                					log.info("Ajout de " + MiscConstants.FISHEYE_USERNAME + " à " + gitlabGroup.getName() + "...");
                					gitlab.getApi().addGroupMember(gitlabGroup, user, GitlabAccessLevel.Reporter);
            				} else {
                					log.debug(MiscConstants.FISHEYE_USERNAME + " est déjà membre du groupe " + gitlabGroup.getName());
            				}
    			    	}
		    	    }
		    	}
		    	else
		    	{
		    	    log.info(MiscConstants.FISHEYE_USERNAME + " is not a Gitlab user...");
		    	}
		} catch (IOException e) {
			log.error("Une erreur est survenue lors de l'itération sur l'un des groupes. L'erreur était : " + e);
		}
	}
}
