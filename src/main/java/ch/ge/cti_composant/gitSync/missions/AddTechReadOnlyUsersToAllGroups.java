package ch.ge.cti_composant.gitSync.missions;

import java.io.IOException;
import java.util.List;

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
 * Ajoute les admins à tous les groupes.
 */
public class AddTechReadOnlyUsersToAllGroups implements Mission {
    	
	private static final Logger LOGGER = LoggerFactory.getLogger(AddTechReadOnlyUsersToAllGroups.class);

	@Override
	public void start(LDAPTree ldapTree, Gitlab gitlab) {
	    addUser(ldapTree, gitlab, MiscConstants.FISHEYE_USERNAME);
	    addUser(ldapTree, gitlab, MiscConstants.MWFL_USERNAME);
	}
	
	private void addUser(LDAPTree ldapTree, Gitlab gitlab, String username)
	{
		try {
		    	GitlabUser user = MissionUtils.getGitlabUser(gitlab.getApi(), username);
		    	
		    	if (user != null)
		    	{
		    	    for (GitlabGroup gitlabGroup : gitlab.getTree().getGroups()) {
			    	
			    	//
			    	// Ne pas le faire pour ***REMOVED*** ni ***REMOVED***
			    	//
			    
			    	if (!"***REMOVED***".equals(gitlabGroup.getName()) && !"***REMOVED***".equals(gitlabGroup.getName()) ) {
			    	    
           				List<GitlabGroupMember> members = gitlab.getApi().getGroupMembers(gitlabGroup.getId());
           				
        				if (!MissionUtils.isGitlabUserMemberOfGroup(members, username)) {
            					LOGGER.info("Ajout de " + username + " �" + gitlabGroup.getName() + "...");
            					gitlab.getApi().addGroupMember(gitlabGroup, user, GitlabAccessLevel.Reporter);
        				} else {
            					LOGGER.info(username + " est d�j�membre du groupe " + gitlabGroup.getName());
        				}
			    	}
		    	    }
		    	}
		    	else
		    	{
		    	    LOGGER.info(username + " is not a Gitlab user");
		    	}
		} catch (IOException e) {
			LOGGER.error("Une erreur est survenue lors de l'it�ration sur l'un des groupes. L'erreur �tait : " + e);
		}
	}
}
