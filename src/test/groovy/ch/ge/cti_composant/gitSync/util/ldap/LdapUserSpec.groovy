package ch.ge.cti_composant.gitSync.util.ldap

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
        e.getMessage().startsWith("An ldap user needs to have a \"cn\" attribute")
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
