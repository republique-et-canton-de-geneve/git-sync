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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.gitlab.api.models.GitlabUser;
import org.gitlab.api.models.GitlabUserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper;
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser;

/**
 * Adds the authorized users to GitLab.
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

	for (GitlabUser gitlabUser : gitlabUsers.values()) {
	    blockOrUnblockUser(api, gitlabUser, ldapUsers);
	}

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

    private void blockOrUnblockUser(GitlabAPIWrapper api, final GitlabUser gitlabUser,
	    Map<String, LdapUser> ldapUsers) {
	if (gitlabUser == null || !fromLdap(gitlabUser)) {
	    // Do nothing
	    return;
	}

	// User state in gitlab
	boolean isActiveGitlab = !"blocked".equals(gitlabUser.getState());

	// User state in ldap
	LdapUser currentLdapUser = ldapUsers.get(gitlabUser.getUsername());
	String loginDisabled = Boolean.FALSE.toString();
	if (currentLdapUser != null) {
	    try {
		loginDisabled = currentLdapUser.getAttribute("loginDisabled");
	    }
	    catch (Exception e) {
		loginDisabled = Boolean.FALSE.toString();
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
