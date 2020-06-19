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
package ch.ge.cti_composant.gitsync.util.gitlab

import ch.ge.cti_composant.gitsync.data.DataProvider
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree
import ch.ge.cti_composant.gitsync.util.ldap.LdapTreeSupport
import org.gitlab.api.models.GitlabGroup
import org.gitlab.api.models.GitlabUser
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link Gitlab}.
 */
@Unroll
class GitLabSpec extends Specification {

	def "#getGroups should return the expected GitLab groups"() {
		given:
		def gitlab = new DataProvider().setupGitlabTree()

		when:
		List<GitlabGroup> groups = gitlab.getGroups().sort{g1, g2 -> g1.getName().compareTo(g2.getName())}

		then:
		groups.size() == 2
		groups.get(0).getName() == "Dev"
		groups.get(1).getName() == "Network"
	}

	def "#getUsers(group) should return the expected GitLab users"() {
        given:
        def gitlab = new DataProvider().setupGitlabTree()
        List<GitlabGroup> groups = gitlab.getGroups().sort{g1, g2 -> g1.getName().compareTo(g2.getName())}
        def devGroup = groups.get(0)

        when:
        Map<String, GitlabUser> users = gitlab.getUsers(devGroup)

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
        users.get("Marie").getName() == "Marie"
    }

	def "#getUsers should return all GitLab users"() {
        given:
        def gitlab = new DataProvider().setupGitlabTree()

        when:
        Map<String, GitlabUser> users = gitlab.getUsers()

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
        users.get("Marie").getName() == "Marie"
    }

    /**
     * Tests class {@link LdapTreeSupport}.
     */
    @Unroll
    static class LdapTreeSupportSpec extends Specification {

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
            users.get("Jean").toString() == "Jean"
            users.get("Marie").toString() == "Marie"
            users.get("Paul").toString() == "Paul"
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

        LdapTree setupLdapTree() {
            return new DataProvider().setupLdapTree()
        }

    }
}
