package ch.ge.cti_composant.gitSync.data

import ch.ge.cti_composant.gitSync.util.ldap.LdapGroup
import ch.ge.cti_composant.gitSync.util.ldap.LdapTree
import ch.ge.cti_composant.gitSync.util.ldap.LdapUser
import ch.ge.cti_composant.gitSync.util.ldap.LdapTreeSupport
import ch.ge.cti_composant.gitSync.util.gitlab.Gitlab
import org.gitlab.api.models.GitlabGroup
import org.gitlab.api.models.GitlabUser

/**
 * Class to provide data used by several Spec classes.
 */
class DataProvider {

    /**
     * Create an ldap tree with groups and users.
     */
    LdapTree setupLdapTree() {
        def net = new LdapGroup("Network")
        def dev = new LdapGroup("Dev")

        def jean = new LdapUser(Collections.singletonMap("cn", "Jean"))
        def marie = new LdapUser(Collections.singletonMap("cn", "Marie"))
        def paul = new LdapUser(Collections.singletonMap("cn", "Paul"))

        def netUsers = new HashMap<String, LdapUser>()
        netUsers.put(marie.name, marie)
        netUsers.put(paul.name, paul)

        def devUsers = new HashMap<String, LdapUser>()
        devUsers.put(paul.name, paul)
        devUsers.put(marie.name, marie)
        devUsers.put(jean.name, jean)

        def tree = new HashMap<LdapGroup, Map<String, LdapUser>>()
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
