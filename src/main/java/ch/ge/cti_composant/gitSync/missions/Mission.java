package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * A mission is an operation performed on the GitLab server, using the LDAP tree.
 */
public interface Mission {

	void start(LdapTree ldapTree, Gitlab gitlab);

}
