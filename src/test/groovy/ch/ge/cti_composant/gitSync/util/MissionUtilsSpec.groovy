/*
 * gitSync
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

    def "#validateLdapGroupExistence should find a matching LDAP group for a GitLab group"() {
        given:
        def ldapTree = new DataProvider().setupLdapTree()

        expect:
        def gitlabGroup = new GitlabGroup()
        gitlabGroup.setName(groupName)
        found == MissionUtils.validateLdapGroupExistence(gitlabGroup, ldapTree)

        where:
        groupName | found
        "Dev"     | true
        "Network" | true
        "Dummy"   | false
    }

}
