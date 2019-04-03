package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.util.LDAP_temp.LDAPTree;
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab;

/**
 * A mission is an operation performed on the GitLab server, using the LDAP_temp tree.
 */
public interface Mission {

	void start(LDAPTree ldapTree, Gitlab gitlab);

}
