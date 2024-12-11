/*
 * gitsync
 *
 * Copyright (C) 2017-2019 République et canton de Genève
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ge.cti_composant.gitsync.missions;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabUser;
import org.gitlab.api.models.GitlabUserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.util.MissionUtils;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

/**
 * Blocks or unblocks users in GitLab (BR5).
 */
public class BlockOrUnblockUsers implements Mission {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockOrUnblockUsers.class);

    @Override
    public void start(LdapTree ldapTree, Gitlab gitlab) {
	LOGGER.info("Block or unblock users in gitlab");

	GitlabAPIWrapper api = gitlab.getApi();

	// Gitlab users
	Map<String, GitlabUser> gitlabUsers = new HashMap<>();
	api.getUsers().forEach(gitlabUser -> gitlabUsers.put(gitlabUser.getUsername(), gitlabUser));

	// Ldap users
	Map<String, LdapUser> ldapUsers = new HashMap<>();
	for (LdapGroup group : ldapTree.getGroups()) {
	    ldapUsers.putAll(ldapTree.getUsers(group));
	}

	gitlabUsers.values().stream()
	    .filter(user -> !MissionUtils.getWideAccessUsers().contains(user.getUsername()))
	    .filter(user -> !MissionUtils.getNotToCleanUsers().contains(user.getUsername()))
	    .sorted(Comparator.comparing(GitlabUser::getUsername))
	    .forEach(user -> blockOrUnblockUser(api, user, ldapUsers));
	    
	LOGGER.info("Block or unblock users in gitlab completed");
    }

    private boolean fromLdap(final GitlabUser gitlabUser) {
	boolean result = false;
	for (GitlabUserIdentity ident : gitlabUser.getIdentities()) {
	    if ("ldapmain".equals(ident.getProvider()) && StringUtils.isNotBlank(ident.getExternUid())) {
		result = true;
		break;
	    }
	}
	return result;
    }

    private String getCnFromLdapIdentity(final GitlabUser gitlabUser) {
	String result = gitlabUser.getUsername();
	for (GitlabUserIdentity ident : gitlabUser.getIdentities()) {
	    if ("ldapmain".equals(ident.getProvider()) && StringUtils.isNotBlank(ident.getExternUid())) {
		String ldapUid = ident.getExternUid().replace("cn=", "");
		ldapUid = ldapUid.substring(0, ldapUid.indexOf(","));
		result = ldapUid;
		break;
	    }
	}
	return result;
    }

    private void blockOrUnblockUser(GitlabAPIWrapper api, final GitlabUser gitlabUser,
	    Map<String, LdapUser> ldapUsers) {
	if (gitlabUser == null || !fromLdap(gitlabUser)) {
	    // Do nothing
	    return;
	}

	// User state in gitlab
	String gitlabUserState = gitlabUser.getState();
	if("ldap_blocked".equals(gitlabUserState)) {
	    // Do nothing, impossible to change state via api
	    return;
	}
	boolean isActiveGitlab = !"blocked".equals(gitlabUserState);

	// User state in ldap
	LdapUser currentLdapUser = ldapUsers.get(gitlabUser.getUsername().toUpperCase(Locale.FRANCE));
	if(currentLdapUser == null) {
	    String cnFromLdapIdentity = getCnFromLdapIdentity(gitlabUser);
	    currentLdapUser = ldapUsers.get(cnFromLdapIdentity.toUpperCase(Locale.FRANCE));
	}
	String loginDisabled;
	if (currentLdapUser != null) {
	    try {
		loginDisabled = currentLdapUser.getAttribute("loginDisabled");
	    }
	    catch (Exception e) {
		loginDisabled = Boolean.TRUE.toString();
	    }
	}
	else {
	    loginDisabled = Boolean.TRUE.toString();
	}
	boolean isActiveLdap = Boolean.FALSE.toString().equalsIgnoreCase(loginDisabled);

	// Active in ldap but blocked in gitlab => unblock user
	if (isActiveLdap && !isActiveGitlab) {
	    LOGGER.info("    Unblock user [{}]", gitlabUser.getUsername());
	    api.unblockUser(gitlabUser.getId());
	}
	// Blocked in ldap but active in gitlab => block user
	else if (!isActiveLdap && isActiveGitlab) {
	    LOGGER.info("    Block user [{}]", gitlabUser.getUsername());
	    api.blockUser(gitlabUser.getId());
	}
    }

}
