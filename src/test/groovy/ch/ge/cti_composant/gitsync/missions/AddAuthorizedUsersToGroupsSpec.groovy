package ch.ge.cti_composant.gitsync.missions

import ch.ge.cti_composant.gitsync.util.MissionUtils
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree
import org.gitlab4j.api.models.AccessLevel
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Member
import org.gitlab4j.api.models.User
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class AddAuthorizedUsersToGroupsSpec extends Specification {
    def standardGroupUsersPattern = MissionUtils.class.getDeclaredField("standardGroupUsersPattern")

    def setup() {
        standardGroupUsersPattern.setAccessible(true)
        standardGroupUsersPattern.set(null, ~/^(?!VSR).+$/)
    }

    def cleanup() {
        standardGroupUsersPattern.setAccessible(true)
        standardGroupUsersPattern.set(null, null)
    }

    def "start"() {
        given:
        def addAuthorizedUsersToGroups = new AddAuthorizedUsersToGroups()

        def user1 = new User().withUsername("user1").withId(1)
        def user2 = new User().withUsername("user2").withId(2)
        def user3 = new User().withUsername("user3").withId(3)
        def user4 = new User().withUsername("user4").withId(4)
        def user5 = new User().withUsername("VSRuser5").withId(5)

        def gitlabUsers = [user1, user2, user3, user5]

        def ldapUsers = [
                (user1.getUsername()): user1,
                (user2.getUsername()): user2,
                (user3.getUsername()): user3,
                (user4.getUsername()): user4,
                (user5.getUsername()): user5
        ]

        def group1 = new Group().withName("group1")

        def group1Members = [
                new Member().withUsername(user1.getUsername()).withId(user1.getId()).withAccessLevel(user1AccessLevel),
                new Member().withUsername(user2.getUsername()).withId(user2.getId()).withAccessLevel(user2AccessLevel)
        ]

        def ldapTree = Mock(LdapTree) {
            getUsers(group1.getName()) >> ldapUsers
        }

        def api = Mock(GitlabAPIWrapper) {
            getUsers() >> gitlabUsers

            getGroupMembers(group1) >> group1Members
        }

        def gitlab = Mock(Gitlab) {
            getApi() >> api
            getGroups() >> [group1]
        }

        when:
        addAuthorizedUsersToGroups.start(ldapTree, gitlab)

        then:
        user1PromotionCount * api.deleteGroupMember(group1, user1.getId())
        user1PromotionCount * api.addGroupMember(group1, user1.getId(), AccessLevel.MAINTAINER)

        user2PromotionCount * api.deleteGroupMember(group1, user2.getId())
        user2PromotionCount * api.addGroupMember(group1, user2.getId(), AccessLevel.MAINTAINER)

        0 * api.deleteGroupMember(group1, user3.getId())
        1 * api.addGroupMember(group1, user3.getId(), AccessLevel.MAINTAINER)

        0 * api.deleteGroupMember(group1, user5.getId())
        0 * api.addGroupMember(group1, user5.getId(), AccessLevel.MAINTAINER)

        where:
        user1AccessLevel       | user2AccessLevel       | user1PromotionCount | user2PromotionCount
        AccessLevel.MAINTAINER | AccessLevel.MAINTAINER | 0                   | 0
        AccessLevel.DEVELOPER  | AccessLevel.MAINTAINER | 1                   | 0
        AccessLevel.MAINTAINER | AccessLevel.DEVELOPER  | 0                   | 1

    }
}
