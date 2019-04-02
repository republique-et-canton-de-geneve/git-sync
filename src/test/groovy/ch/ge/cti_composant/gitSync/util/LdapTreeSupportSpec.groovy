package ch.ge.cti_composant.gitSync.util

import ch.ge.cti_composant.gitSync.data.DataProvider
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPTree
import ch.ge.cti_composant.gitSync.util.LDAP.LDAPUser
import ch.ge.cti_composant.gitSync.util.LDAP.LdapTreeSupport
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link LdapTreeSupport}.
 */
@Unroll
class LdapTreeSupportSpec extends Specification {

    def "#getGroups should return the expected groups containing the expected users"() {
        given:
        def ldapTree = setupLdapTree()

        when:
        def groups = ldapTree.getGroups()

        then:
        groups.size() == 2
        groups.get(0).getName() == "Dev"
        groups.get(1).getName() == "Network"
    }

    def "#getUsers should return the expected users"() {
        given:
        def ldapTree = setupLdapTree()

        when:
        def groupDev = ldapTree.getGroups().get(0)
        def users = ldapTree.getUsers(groupDev)

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
        users.get("Jean").getName() == "Jean"
        users.get("Marie").getName() == "Marie"
        users.get("Paul").getName() == "Paul"
    }

    def "#getUsers(groupName) should behave like #getUsers(group)"() {
        given:
        def ldapTree = setupLdapTree()

        when:
        def users = ldapTree.getUsers("Dev")

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
    }

    LDAPTree setupLdapTree() {
        return new DataProvider().setupLdapTree()
    }

}
