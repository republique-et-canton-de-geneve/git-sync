Test

Build GitHub :

[![Build with GitHub](https://github.com/republique-et-canton-de-geneve/git-sync/actions/workflows/maven.yml/badge.svg)](https://github.com/republique-et-canton-de-geneve/git-sync/actions/workflows/maven.yml)

Analyse SonarCloud :

[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=bugs)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=code_smells)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=coverage)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=republique-et-canton-de-geneve_git-sync&metric=alert_status)](https://sonarcloud.io/dashboard?id=republique-et-canton-de-geneve_git-sync)

Licence :

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/why-affero-gpl.html)


gitSync is a standalone Java application which replicates the users from an LDAP source into a
[GitLab](https://about.gitlab.com) server.

# Presentation

## Context

At État de Genève we use an Active Directory server to authenticate users, plus a home-made solution
(hereafter denoted as "the LDAP server" ) to manage authorizations.
As is done in many organizations, the LDAP server organizes users in groups, such
as "IT-DEV-JAVA", "IT-DEV-PHP" or "FINANCE".
In the LDAP server, access rights to systems and applications are granted to groups or to users.
A user is assigned to any number of groups.

Now GitLab comes into play.
At État de Genève, the GitLab server is an on-premise instance of the GitLab community edition.
Our need is to *replicate automatically the LDAP configuration of the users onto the GitLab server*.
For example, we want the GitLab server to acquire automatically groups "IT-DEV-JAVA', 'IT-DEV-PHP', etc.,
from the LDAP server, as well as the members of every group.
We also want the GitLab group members to acquire the adequate right permissions.

# Functional description

This section specifies the expected behavior of the application.

## Definitions

### Dry run mode

The parameter `dry-run` allows you to run the application without incurring any modification in GitLab.
Only logs are displayed so you can check future modifications before to set `dry-run` to false.
If the parameter is absent, False is the default value.

### Standard groups and administrator group

By default, a LDAP group is considered as a "standard" groups. One group can be an "administrator" group.
For example, LDAP group "IT-DEV-JAVA" is a standard group, whereas LDAP group "ADMIN" is the
administrator group.
The name of the administrator group, if any, is supplied as parameter `admin-group` in the application's configuration file.
The name of the standard groups must follow the naming convention defined by the parameter `standard.groups` (regex pattern).

### GitLab users not to be cleaned

Some special GitLab users will not be removed from GitLab groups, even if they have been removed from the
corresponding LDAP role.
These users are usually technical users.
At État de Genève, such a wide-access GitLab user has been created to allow a
[Fisheye](https://www.atlassian.com/software/fisheye) server to log on to GitLab and to freely retrieve commits
from any Git repository.
The list of wide-access GitLab users, if any, is supplied as parameter `not-to-clean-users`
in the application's configuration file.

### Blacklisted groups

For some business rules below, some LDAP groups will be explicitly omitted. 
The list of blacklisted LDAP groups, if any, is supplied as parameter `black-listed-groups`
in the application's configuration file.

### Limited-access groups

At État de Genève the baseline is to provide read-only access (Developer role) to all developers.
That is, every developer is able to discover code, clone and create merge requests, even on code that is outside
their working group.
For historical reasons, some groups must remain preserved from such a wide access.
The list of such limited-access groups, if any, is supplied as parameter `limited-access-groups` in the
application's configuration file.

## Business rules

This section lists the actions that are carried out at every execution of the application.

### BR1 : create GitLab groups

For every standard LDAP group, create a GitLab group (hereafter coined the "matching GitLab group") with the same name,
if such group does not exist yet.
 
### BR2 : manage Maintainers

For every standard LDAP group, retrieve the list of users (LU).
  * For every user in list LU:
    * If the user does not exist in GitLab, do nothing
      (see section [GitLab authentication](#gitlab-authentication)).
    * If the user already exists in GitLab,
      is internal and is not assigned to the matching GitLab group, 
      assign it with the "Maintainer" GitLab role.
    * If the user already exists in GitLab and is already assigned to the matching GitLab group, do nothing.
  * Additionally:  
    * If any user already exists in GitLab, is assigned to the matching GitLab group, has "Maintainer" role,
      but is not in list LU, remove it from the GitLab group,
      unless the user belongs to the list of not-to-be-cleaned users
    * If any user already exists in GitLab, is assigned to the matching GitLab group,
      has either "Maintainer" or "Developer" role"
      and is external, remove it from the GitLab group.

### BR3 : set all others users are Developers

Retrieve the list of users (LU) of all standard GitLab groups:
  * For every user in list LU:
    * For every GitLab group not in `limited-access-groups`:
      * If the user is not assigned to the GitLab group
        and is an internal GitLab user,
        assign it with the "Developer" GitLab role.
      * If the user is already assigned to the GitLab group with a role weaker than "Developer"
        and is an internal GitLab user,
        assign it with the "Developer" GitLab role.
      * If the user is already assigned to the GitLab group with a role stronger than or equal to "Developer",
        do nothing.

### BR4 : manage administrators

For the administrator LDAP group (if any, defined by `admin-group`), retrieve the list of users. For every user in the list:
  * If the user does not exist in GitLab, do nothing
    (see section [GitLab authentication](#gitlab-authentication)).
  * If the user already exists in GitLab:
    * Give it the Admin (as opposed to Regular) access level.

### BR5 : manage Owners

For the owner LDAP group (if any, defined by `owner-group`), retrieve the list of users. For every user in the list:
  * If the user does not exist in GitLab, do nothing
    (see section [GitLab authentication](#gitlab-authentication)).
  * If the user exists in GitLab and the user is not a GitLab administrator:
    * Assign it to all non-administrator groups (with Owner role permission), except the groups in a
      black list supplied as a parameter to the application.

### BR6 : block or unblock users

Retrieve the list of users (LU) of all standard GitLab groups:
  * For every user in list LU:
    * If the user is blocked in Gitlab and is active in LDAP, unblock the user
    * If the user is active in Gitlab and is inactive in LDAP, block the user

### Notes
  * The business rules for the standard groups (BR2) are applied before the business rules for the
    administrator group (BR4), otherwise GitLab users having Admin access level would end up being
    downgraded to Regular access level.
  * In all business rules here above, whenever a list of users (LU) is mentioned, only the users
    whose name comply with the regular expression supplied by the parameter `standard-group-users`
    are considered. The other users are ignored.

## Remarks

* The application does not create GitLab users.
* The application does not create GitLab sub-groups. It only creates GitLab groups.
* Whenever the application creates a GitLab group, the GitLab role permission "Owner" group is automatically
  assigned by GitLab to the user used for the connection.
  That user is the user that matches the connection token ``gitlab.account.token`` to be supplied in the
  configuration file.
* If an existing GitLab group matches no LDAP group, nothing happens to it.
* The replication is one-way: from the LDAP server to the GitLab server. Accordingly, read-only access
  to the LDAP server is sufficient. 

## GitLab authentication

The application deals with authorization only, not with authentication.
User authentication on the GitLab server is configured directly on the GitLab server. 
The authentication provider is typically an LDAP server, possibly the one the application extracts its data from. 

At État de Genève, end users known to the LDAP server but still unknown to the GitLab server acquire their
configuration on the GitLab server by means of the following sequence of operations:
1. On a browser the user navigates to the GitLab server.
   This performs authentication.
   In response the GitLab displays a blank page, which leads the user to believe that connexion went wrong, but
   this weird behavior is expected.
   Behind the scenes GitLab creates a user with access level "Regular" (as opposed to "Admin").
2. Next time the application is run, it loads the user's configuration into GitLab.
3. When the user navigates again to the GitLab server, they readily see all projects they have access to.
   For example, if the user belongs to groups "GROUP-IT-DEV" and "GROUP-IT-NETWORK", they will have access
   to all Git projects defined in these 2 groups as well as in their sub-groups. 

For each user, the hand-shake step 1 above occurs only once. 

# Technical facts

The application is a standalone application. It performs the replication once and then exits.
It requires Java 21 to build and run.

Communication with the LDAP server is performed by means of a home-made Java library named
[gina-ldap-client](https://github.com/republique-et-canton-de-geneve/gina-ldap-client)
(in French) which internally resorts to the standard `javax.naming.ldap` API.
That library is specifically tuned to the LDAP data model of Gina, the État de Genève's LDAP server.
Transforming the application to connect to another LDAP server than Gina entails the following changes:
* Creating a class, say, `CustomLdapTreeBuilder` that implements interface
  [LdapTreeBuilder](src/main/java/ch/ge/cti_composant/gitsync/util/ldap/LdapTreeBuilder.java);
* In the top class
  [GitSync](src/main/java/ch/ge/cti_composant/gitsync/GitSync.java), replacing the usage of
  [GinalLdapTreeBuilder](src/main/java/ch/ge/cti_composant/gitsync/util/ldap/gina/GinaLdapTreeBuilder.java)
  with that of `CustomLdapTreeBuilder`;
* In the properties file [configuration.properties](./configuration.properties),
  replacing the settings of the Gina LDAP server with those of the custom LDAP server.

Communication with the GitLab server is performed by means of GitLab's own
[java-gitlab-api](https://mvnrepository.com/artifact/org.gitlab/java-gitlab-api)
library, which internally resorts to REST services. 
The application is compatible with any GitLab server, for example GitLab 11.X or GitLab 14.X, 
that complies with version 4 of the GitLab API.

The application stores the whole LDAP configuration of the groups in memory. For a few hundred users this has not
caused any trouble so far.

If some glitch happens when extracting the user configuration from the LDAP server (for example, the data have
been moved to another zone of the LDAP tree) and the application comes up with an empty configuration, replication
to the GitLab server will simply clean up the user assignments, thereby preventing all users from
accessing their projects.
In order to prevent such bad situation from occurring, an extra configuration parameter `minimum-user-count`
is provided:
if the user configuration contains fewer users than the value of the parameter, the application exits without performing
the replication.

# Build

Run

``mvn clean install``

Starting from version 3.10, the dependency on `gina-ldap-client` 4.2.0 is no longer available in Maven Central.
So if you are a developer from outside État de Genève, before running the above command,
you will have to clone
[projet gina-ldap-client](https://github.com/republique-et-canton-de-geneve/gina-ldap-client)
from GitLab, then build it (`mvn install`), then modify the POM of this projet to make it depend on your
snapshot version of `gina-ldap-client` instead of the release version.

# Execution

The mapping is carried out by running the application:

``
java -jar target/gitSync-XXX-SNAPSHOT.jar configuration.properties
``

Copy the file [configuration-base.properties](./configuration-base.properties) to configuration.properties and update it with the appropriate parameter values.
The unique parameter is the path to the configuration file.
The parameters in the supplied file configuration.properties must be adapted.

Logging is performed by means of Logback. A basic configuration file `logback.xml` is included in the source
tree and is present in the JAR file created by the `mvn install` command.
It is sufficient for standard analysis, although some people might find the INFO-level console output too verbose.

At État de Genève, execution typically takes a few minutes to execute. It processes
about 100 groups encompassing 1000 group users.

Practical usage requires spawning the application regularly, for example every hour.
This can be done with a crontab-like job.
