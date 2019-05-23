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
 * Tests class {@link LdapUser}.
 */
@Unroll
class LdapUserSpec extends Specification {

    def "constructor should fail when attribute \"cn\" is absent"() {
        given:
        def attributes = Collections.singletonMap("not cn", "Jean Dupont")

        when:
        new LdapUser(attributes)

        then:
        IllegalStateException e = thrown()
        e.getMessage().startsWith("An LDAP user needs to have a \"cn\" attribute")
    }

    def "#getName should return something when attribute \"cn\" is present"() {
        given:
        def attributes = Collections.singletonMap("cn", "Jean Dupont")

        when:
        def user = new LdapUser(attributes)

        then:
        user.getName() == "Jean Dupont"
    }

    def "#getAttribute should succeed for an existing attribute"() {
        given:
        def attributes = new HashMap()
        attributes.put("cn", "Jean Dupont")
        attributes.put("ou", "IT")
        def user = new LdapUser(attributes)

        expect:
        user.getAttribute(key) == value

        where:
        key  | value
        "cn" | "Jean Dupont"
        "ou" | "IT"
    }

    def "#getAttribute should fail for an non existing attribute"() {
        given:
        def attributes = Collections.singletonMap("cn", "Jean Dupont")

        when:
        def user = new LdapUser(attributes)

        then:
        user.getName() == "Jean Dupont"
    }

    def "#hashCode should return the correct value"() {
        given:
        def attributes = Collections.singletonMap("cn", "Jean Dupont")

        when:
        def user = new LdapUser(attributes)

        then:
        user.hashCode() == 1396370574
    }

    def "#equals should compare correctly"() {
        given:
        def attributesA = Collections.singletonMap("cn", "Jean Dupont")
        def attributesC = Collections.singletonMap("cn", "Marie Dupont")

        when:
        def userA = new LdapUser(attributesA)
        def userB = new LdapUser(attributesA)
        def userC = new LdapUser(attributesC)

        then:
        userA == userB
        userA != userC
        !userA.equals(null)
        userA != "Jean Dupont"
    }

}
