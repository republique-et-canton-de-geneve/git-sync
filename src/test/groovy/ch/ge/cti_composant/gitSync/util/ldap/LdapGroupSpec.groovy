package ch.ge.cti_composant.gitSync.util.ldap

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
        !groupA.equals(null)
        groupA != "Hello"
    }

}
