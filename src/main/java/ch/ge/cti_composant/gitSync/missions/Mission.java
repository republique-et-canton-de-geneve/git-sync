package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * A mission is an operation performed on a GitLab server, using an LDAP tree.
 */
public interface Mission {

	void start(LDAPTree ldapTree, Gitlab gitlab);

}
