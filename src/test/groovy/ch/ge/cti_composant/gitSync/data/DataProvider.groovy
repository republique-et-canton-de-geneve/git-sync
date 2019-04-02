package ch.ge.cti_composant.gitSync.data

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser
import ch.ge.cti_composant.gitSync.util.LDAP.LdapTreeSupport
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab
import org.gitlab.api.models.GitlabGroup
import org.gitlab.api.models.GitlabUser

/**
 * Class to provide data used by several Spec classes.
 */
class DataProvider {

    /**
     * Create an LDAP tree with groups and users.
     */
    LDAPTree setupLdapTree() {
        def net = new LDAPGroup("Network")
        def dev = new LDAPGroup("Dev")

        def jean = new LDAPUser(Collections.singletonMap("cn", "Jean"))
        def marie = new LDAPUser(Collections.singletonMap("cn", "Marie"))
        def paul = new LDAPUser(Collections.singletonMap("cn", "Paul"))

        def netUsers = new HashMap<String, LDAPUser>()
        netUsers.put(marie.name, marie)
        netUsers.put(paul.name, paul)

        def devUsers = new HashMap<String, LDAPUser>()
        devUsers.put(paul.name, paul)
        devUsers.put(marie.name, marie)
        devUsers.put(jean.name, jean)

        def tree = new HashMap<LDAPGroup, Map<String, LDAPUser>>()
        tree.put(net, netUsers)
        tree.put(dev, devUsers)

        return new LdapTreeSupport(tree)
    }

    /**
     * Create a GitLab tree with groups and users.
     */
    Gitlab setupGitlabTree() {
        // groups
        def net = new GitlabGroup()
        net.setName("Network")
        def dev = new GitlabGroup()
        dev.setName("Dev")

        // users
        def jean = new GitlabUser()
        jean.setName("Jean")
        def marie = new GitlabUser()
        marie.setName("Marie")
        def paul = new GitlabUser()
        paul.setName("Paul")

        // tree
        def netUsers = new HashMap<String, GitlabUser>()
        netUsers.put(marie.name, marie)
        netUsers.put(paul.name, paul)

        def devUsers = new HashMap<String, GitlabUser>()
        devUsers.put(paul.name, paul)
        devUsers.put(marie.name, marie)
        devUsers.put(jean.name, jean)

        def tree = new HashMap<GitlabGroup, Map<String, GitlabUser>>()
        tree.put(net, netUsers)
        tree.put(dev, devUsers)

        // gitlab
        def gitlab = new Gitlab(tree, "someUrl", "someApiKey")
        return gitlab
    }

}
