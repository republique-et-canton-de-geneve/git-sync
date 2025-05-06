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
package ch.ge.cti_composant.gitsync.data

import ch.ge.cti_composant.gitsync.util.ldap.LdapGroup
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree
import ch.ge.cti_composant.gitsync.util.ldap.LdapUser
import ch.ge.cti_composant.gitsync.util.ldap.LdapTreeSupport
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab
import org.gitlab4j.api.models.Group

/**
 * Class to provide data used by several Spec classes.
 */
class DataProvider {

    /**
     * Create an ldap tree with groups and users.
     */
    static LdapTree setupLdapTree() {
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
    static Gitlab setupGitlabTree() {
        // groups
        def net = new Group()
        net.setName("Network")
        def dev = new Group()
        dev.setName("Dev")

        def tree = new HashSet<Group>()
        tree.add(net)
        tree.add(dev)

        // gitlab
        def gitlab = new Gitlab(tree, "someUrl", "someApiKey")
        return gitlab
    }

}
