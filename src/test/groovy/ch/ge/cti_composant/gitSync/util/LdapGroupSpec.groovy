package ch.ge.cti_composant.gitSync.util

import ch.ge.cti_composant.gitSync.util.LDAP.LDAPGroup
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link LDAPGroup}.
 */
@Unroll
class LdapGroupSpec extends Specification {

    def "#getName should return the correct value"() {
        when:
        def group = new LDAPGroup("Dev")

        then:
        group.name == "Dev"
    }

    def "#hashCode should return the correct value"() {
        when:
        def group = new LDAPGroup("Dev")

        then:
        group.hashCode() == 68597
    }

    def "#equals should compare correctly"() {
        when:
        def groupA = new LDAPGroup("Dept A")
        def groupB = new LDAPGroup("Dept A")
        def groupC = new LDAPGroup("Dept C")

        then:
        groupA == groupB
        groupA != groupC
        !groupA.equals(null)
        groupA != "Hello"
    }

}
