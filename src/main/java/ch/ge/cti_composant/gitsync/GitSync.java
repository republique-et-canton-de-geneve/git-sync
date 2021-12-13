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
package ch.ge.cti_composant.gitsync;

import ch.ge.cti_composant.gitsync.missions.AddAuthorizedUsersToGroups;
import ch.ge.cti_composant.gitsync.missions.AddTechReadOnlyUsersToAllGroups;
import ch.ge.cti_composant.gitsync.missions.CheckMinimumUserCount;
import ch.ge.cti_composant.gitsync.missions.CleanGroupsFromUnauthorizedUsers;
import ch.ge.cti_composant.gitsync.missions.PromoteAdminUsers;
import ch.ge.cti_composant.gitsync.missions.PropagateAdminUsersToAllGroups;
import ch.ge.cti_composant.gitsync.service.GitlabService;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.ldap.LdapTreeBuilder;
import ch.ge.cti_composant.gitsync.util.ldap.gina.GinaLdapTreeBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Top-level class of the application: extracts the groups and users from the LDAP server and assigns them as groups
 * and users to the GitLab server.
 */
public class GitSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitSync.class);

    private static final Properties props = new Properties();

    private LdapTree ldapTree = null;

    private Gitlab gitlab = null;

    /**
     * Performs all operations.
     */
    public void run(String path) {
        try {
            loadProperties(path);

            LOGGER.info("PHASE 1: Set up the in-memory LDAP tree");
            setupLdap();

            LOGGER.info("PHASE 2: Set up the in-memory GitLab tree");
            setupGitLab();

            LOGGER.info("PHASE 3: Apply the business rules");
            applyRules();

        } catch (Exception e) {
            LOGGER.error("Exception caught while processing the LDAP/GitLab trees", e);
        }
    }

    /**
     * Loads the properties file.
     */
    private void loadProperties(String path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(Paths.get(path))) {
            props.load(inputStream);
        }

        LOGGER.info("Running GitSync {} with LDAP server [{}] and GitLab server [{}]",
                getVersion(),
                props.get("gina-ldap-client.ldap-server-url"),
                props.get("gitlab.hostname"));
    }

    /**
     * Sets up the in-memory tree of LDAP groups and LDAP users.
     */
    private void setupLdap() {
        // If you need to load the data from another LDAP server than Etat de Geneve's LDAP server, you must
        // replace the treeBuilder below with a custom one. See file README.md.
        LdapTreeBuilder treeBuilder = new GinaLdapTreeBuilder();
        ldapTree = treeBuilder.createTree();
    }

    /**
     * Sets up the in-memory tree of GitLab groups and GitLab users.
     */
    private void setupGitLab() {
        gitlab = new GitlabService().buildGitlabContext(
                props.getProperty("gitlab.hostname"),
                props.getProperty("gitlab.account.token"),
                ldapTree);
    }

    /**
     * Performs the missions.
     */
    private void applyRules() {
        // precaution: do not take the risk to clear up GitLab with an empty set of groups and users
        new CheckMinimumUserCount().start(ldapTree, gitlab);

        // remove the non-authorized users
        new CleanGroupsFromUnauthorizedUsers().start(ldapTree, gitlab);

        // add the authorized users (new permissions)
        new AddAuthorizedUsersToGroups().start(ldapTree, gitlab);

        // add the Admins
        new PromoteAdminUsers().start(ldapTree, gitlab);

        // add the Admins to all groups
        new PropagateAdminUsersToAllGroups().start(ldapTree, gitlab);

        // add read-only permission to specific wide-access users on all groups
        new AddTechReadOnlyUsersToAllGroups().start(ldapTree, gitlab);
    }

    /**
     * Returns the specified property, or null if not found.
     */
    public static String getProperty(String name) {
        return props.getProperty(name);
    }

    /**
     * Returns the Maven version of the JAR.
     */
    private static String getVersion() {
        Properties properties = new Properties();
        String filePath = "/META-INF/maven/ch.ge.cti.composant/gitSync/pom.properties";
        String versionName = "version";
        String versionValue;

        try {
            properties.load(GitSync.class.getResourceAsStream(filePath));
            versionValue = properties.getProperty(versionName);
            if (StringUtils.isBlank(versionValue)) {
                LOGGER.warn("No property [{}] in file [{}]", versionName, filePath);
            }
            return versionValue;
        } catch (Exception e) {
            LOGGER.warn("Could not read property [{}] in file [{}]", versionName, filePath, e);
            return "";
        }
    }

}
