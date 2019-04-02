package ch.ge.cti_composant.gitSync.util

import ch.ge.cti_composant.gitSync.data.DataProvider
import org.gitlab.api.models.GitlabGroup
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link MissionUtils}.
 */
@Unroll
class MissionUtilsSpec extends Specification {

    def "#validateLDAPGroupExistence should find a matching LDAP role for a GitLab group"() {
        given:
        def ldapTree = new DataProvider().setupLdapTree()

        expect:
        def gitlabGroup = new GitlabGroup()
        gitlabGroup.setName(groupName)
        found == MissionUtils.validateLDAPGroupExistence(gitlabGroup, ldapTree)

        where:
        groupName | found
        "Dev"     | true
        "Network" | true
        "Dummy"   | false
    }

}
