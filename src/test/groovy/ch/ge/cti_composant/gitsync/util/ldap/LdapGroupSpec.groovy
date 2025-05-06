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
package ch.ge.cti_composant.gitsync.util.ldap

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link LdapGroup}.
 */
@Unroll
class LdapGroupSpec extends Specification {

    def "#getName should return the correct value"() {
        when:
        def group = new LdapGroup("Dev")

        then:
        group.name == "Dev"
    }

    def "#hashCode should return the correct value"() {
        when:
        def group = new LdapGroup("Dev")

        then:
        group.hashCode() == 68597
    }

    def "#equals should compare correctly"() {
        when:
        def groupA = new LdapGroup("Dept A")
        def groupB = new LdapGroup("Dept A")
        def groupC = new LdapGroup("Dept C")

        then:
        groupA == groupB
        groupA != groupC
        groupA != null
        groupA.name != "Hello"
    }

}
