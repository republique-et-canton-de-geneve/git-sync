package ch.ge.cti_composant.gitSync.util.ldap;

/**
 * A object responsible to create a completely initialized {@link LdapTree}.
 */
public interface LdapTreeBuilder {

    /**
     * Creates and initializes an {@LdapTree}.
     */
    LdapTree createTree();

}