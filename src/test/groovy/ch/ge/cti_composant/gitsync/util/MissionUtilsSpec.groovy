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
package ch.ge.cti_composant.gitsync.util

import ch.ge.cti_composant.gitsync.util.exception.GitSyncException
import ch.ge.cti_composant.gitsync.util.gitlab.GitlabAPIWrapper
import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser
import org.gitlab4j.api.models.AccessLevel
import org.gitlab4j.api.models.Member
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.User
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link MissionUtils}.
 */
@Unroll
class MissionUtilsSpec extends Specification {
    def standardGroupsPattern = MissionUtils.class.getDeclaredField("standardGroupsPattern")
    def standardGroupUsersPattern = MissionUtils.class.getDeclaredField("standardGroupUsersPattern")

    def setup() {
        standardGroupsPattern.setAccessible(true)
        standardGroupsPattern.set(null, ~/^[a-zA-Z_]+$/)

        standardGroupUsersPattern.setAccessible(true)
        standardGroupUsersPattern.set(null, ~/^user_[a-z]+$/)
    }

    def cleanup() {
        standardGroupsPattern.setAccessible(true)
        standardGroupsPattern.set(null, null)

        standardGroupUsersPattern.setAccessible(true)
        standardGroupUsersPattern.set(null, null)
    }


    def "validateLdapGroupExistence should find a matching LDAP group for a GitLab group"() {
        given:
        def net = new LdapGroup("Network")
        def dev = new LdapGroup("Dev")
        def ldapTree = Mock(LdapTree)
        ldapTree.getGroups() >> [net, dev]

        expect:
        def gitlabGroup = new Group()
        gitlabGroup.setName(groupName)
        found == MissionUtils.validateLdapGroupExistence(gitlabGroup, ldapTree)

        where:
        groupName | found
        "Dev"     | true
        "Network" | true
        "Dummy"   | false
    }

    def "validateGitlabGroupExistence should return true when group exists in GitLab"() {
        given:
        def ldapGroup = new LdapGroup(groupName)
        def api = Mock(GitlabAPIWrapper)
        api.getGroup(groupName) >> foundGroup

        expect:
        found == MissionUtils.validateGitlabGroupExistence(ldapGroup, api)

        where:
        groupName | foundGroup  | found
        "Dev"     | new Group() | true
        "Network" | null        | false
    }

    def "validateGroupNameCompliantStandardGroups should return true when group name is compliant with standard groups"() {
        given:
        def ldapGroup = new LdapGroup(groupName)

        expect:
        found == MissionUtils.validateGroupNameCompliantStandardGroups(ldapGroup)

        where:
        groupName | found
        "Dev"     | true
        "Network" | true
        "@test"   | false
        "_test"   | true
    }

    def "validateGroupNameCompliantStandardGroups(String) should return true for matching regex"() {
        expect:
        MissionUtils.validateGroupNameCompliantStandardGroups(groupName) == expected

        where:
        groupName | expected
        "Dev"     | true
        "_team"   | true
        "_team1"  | false
        "!!bad"   | false
        ""        | false
    }

    def "isUserCompliant should validate user name with default regex pattern"() {
        given:
        standardGroupUsersPattern.setAccessible(true)
        standardGroupUsersPattern.set(null, null)

        expect:
        MissionUtils.isUserCompliant(username) == expected

        where:
        username    | expected
        "user_john" | true
        "user123"   | true
        "admin"     | true
        ""          | false
        "@test"     | false
    }

    def "isUserCompliant should validate user name with regex pattern"() {
        expect:
        MissionUtils.isUserCompliant(username) == expected

        where:
        username    | expected
        "user_john" | true
        "user123"   | false
        "admin"     | false
        ""          | false
    }

    def "getLdapUsers returns a set of users from the LDAP tree"() {
        given:
        def ldapGroup = new LdapGroup("Dev")
        def ldapTree = Mock(LdapTree)
        def user1 = new LdapUser(Collections.singletonMap("cn", "user1"))
        def user2 = new LdapUser(Collections.singletonMap("cn", "user2"))
        ldapTree.getGroups() >> [ldapGroup]
        ldapTree.getUsers(ldapGroup) >> Map.of(
                user1.getName(), user1,
                user2.getName(), user2
        )

        when:
        def users = MissionUtils.getLdapUsers(ldapTree)

        then:
        users.size() == 2
        users.contains(user1)
        users.contains(user2)
    }

    def "getWideAccessUsers returns a list of usernames with wide access"() {
        when: "wide-access-users property is empty"
        def users = MissionUtils.getWideAccessUsers()

        then: "no users should be returned"
        users.size() == 0
    }

    def "getNotToCleanUsers returns a list of usernames with wide access"() {
        when: "not-to-clean-users property is empty"
        def users = MissionUtils.getNotToCleanUsers()

        then: "no users should be returned"
        users.size() == 0
    }


    def "getLimitedAccessGroups returns a list of groups with wide access"() {
        when: "limited-access-groups property is empty"
        def users = MissionUtils.getLimitedAccessGroups()

        then: "no groups should be returned"
        users.size() == 0
    }

    def "getBlackListedGroups returns a list of groups with wide access"() {
        when: "limited-access-groups property is empty"
        def users = MissionUtils.getBlackListedGroups()

        then: "no groups should be returned"
        users.size() == 0
    }

    def "getOwnerGroup returns the value of the property owner-group"() {
        when: "owner-group property is empty"
        def group = MissionUtils.getOwnerGroup()

        then: "null should be returned"
        group == null
    }

    def "getAdministratorGroup returns the value of the property admin-group"() {
        when: "admin-group property is empty"
        def group = MissionUtils.getAdministratorGroup()

        then: "null should be returned"
        group == null
    }

    def "validateGitlabGroupMemberHasMinimumAccessLevel"() {
        given:
        def isValid = MissionUtils.validateGitlabGroupMemberHasMinimumAccessLevel(members, user, accessLevel)

        expect:
        isValid == expected

        where:
        members                                                                      | user        | accessLevel            | expected
        [new Member().withUsername("user1").withAccessLevel(AccessLevel.MAINTAINER)] | "user1"     | AccessLevel.MAINTAINER | true
        [new Member().withUsername("user1").withAccessLevel(AccessLevel.MAINTAINER)] | "user1"     | AccessLevel.DEVELOPER  | true
        [new Member().withUsername("user2").withAccessLevel(AccessLevel.MAINTAINER)] | "user1"     | AccessLevel.MAINTAINER | false
        [new Member().withUsername("user1").withAccessLevel(AccessLevel.DEVELOPER)]  | "user1"     | AccessLevel.MAINTAINER | false
        [new Member().withUsername("adminUser").withAccessLevel(AccessLevel.ADMIN)]  | "adminUser" | AccessLevel.MAINTAINER | true
    }

    def "isGitlabUserMemberOfGroup"() {
        given:
        def isUserMemberOfGroup = MissionUtils.isGitlabUserMemberOfGroup(members, username)

        expect:
        isUserMemberOfGroup == expected

        where:
        members                                                                      | username | expected
        [new Member().withUsername("user1").withAccessLevel(AccessLevel.MAINTAINER)] | "user1"  | true
        [new Member().withUsername("user1").withAccessLevel(AccessLevel.MAINTAINER)] | "user2"  | false
    }

    def "getAllGitlabUsers"() {
        given:
        def api = Mock(GitlabAPIWrapper)
        api.getUsers() >> users
        def gitlabUsers = MissionUtils.getAllGitlabUsers(api)

        expect:
        gitlabUsers.size() == expected

        where:
        users                                                                | expected
        [new User().withUsername("user1"), new User().withUsername("user2")] | 2
        [new User().withUsername("user1"), new User().withUsername("user1")] | 1
        [new User().withUsername("user1")]                                   | 1
    }

    def "isGitlabUserAdmin"() {
        given:
        GitlabAPIWrapper api = Mock(GitlabAPIWrapper)
        LdapTree ldapTree = Mock(LdapTree)

        def isGitlabUserAdmin = MissionUtils.isGitlabUserAdmin(user, api, ldapTree)

        expect:
        isGitlabUserAdmin == expected

        where:
        user                                               | ldapUsers                                         | expected
        new User().withUsername("user1").withIsAdmin(true) | Map.of("user1", new User().withUsername("user1")) | true
    }

    def "validateGitlabUserExistence"() {
        given:
        def userExistsInGitlab = MissionUtils.validateGitlabUserExistence(ldapUser, gitlabUsers)

        expect:
        userExistsInGitlab == expected

        where:
        ldapUser                                              | gitlabUsers                        | expected
        new LdapUser(Collections.singletonMap("cn", "user1")) | [new User().withUsername("user1")] | true
        new LdapUser(Collections.singletonMap("cn", "user2")) | [new User().withUsername("user1")] | false
    }

    def "validateGitlabUserExistence throw exception"() {
        when: "it finds multiple gitlab user with the same username as the ldap user"
        def ldapUser = new LdapUser(Collections.singletonMap("cn", "user1"))
        def gitlabUsers = [new User().withUsername("user1"), new User().withUsername("user1")]
        MissionUtils.validateGitlabUserExistence(ldapUser, gitlabUsers)

        then: "it should throw a GitSyncException"
        thrown(GitSyncException)
    }
}