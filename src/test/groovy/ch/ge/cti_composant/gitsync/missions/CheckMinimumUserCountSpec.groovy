package ch.ge.cti_composant.gitsync.missions

import ch.ge.cti_composant.gitsync.util.exception.GitSyncException
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree
import org.gitlab4j.api.models.User
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class CheckMinimumUserCountSpec extends Specification {
    def "start with not enough users detected"() {
        given:
        def checkMinimumUserCount = new CheckMinimumUserCount()

        def group1 = Mock(LdapGroup)
        def group2 = Mock(LdapGroup)

        def user1 = Mock(User)
        def user2 = Mock(User)

        def ldapTree = Mock(LdapTree) {
            getGroups() >> [group1, group2]
            getUsers(group1) >> [(user1.getUsername()): user1]
            getUsers(group2) >> [(user2.getUsername()): user2]
        }
        def gitlab = Mock(Gitlab)

        when:
        checkMinimumUserCount.start(ldapTree, gitlab)

        then: "it should throw a GitSyncException"
        thrown(GitSyncException)
    }

    def "start with enough users detected"() {
        given:
        def checkMinimumUserCount = new CheckMinimumUserCount()

        def group1 = Mock(LdapGroup)
        def group2 = Mock(LdapGroup)

        def user1 = Mock(User) {
            getUsername() >> "user1"
        }
        def user2 = Mock(User) {
            getUsername() >> "user2"
        }
        def user3 = Mock(User) {
            getUsername() >> "user3"
        }
        def user4 = Mock(User) {
            getUsername() >> "user4"
        }
        def user5 = Mock(User) {
            getUsername() >> "user5"
        }
        def user6 = Mock(User) {
            getUsername() >> "user6"
        }

        def ldapTree = Mock(LdapTree) {
            getGroups() >> [group1, group2]
            getUsers(group1) >> [
                    (user1.getUsername()): user1,
                    (user2.getUsername()): user2
            ]
            getUsers(group2) >> [
                    (user3.getUsername()): user3,
                    (user4.getUsername()): user4,
                    (user5.getUsername()): user5,
                    (user6.getUsername()): user6
            ]
        }
        def gitlab = Mock(Gitlab)

        when:
        checkMinimumUserCount.start(ldapTree, gitlab)

        then: "it should not throw a GitSyncException"
        notThrown(GitSyncException)
    }
}
