gitSync is a standalone Java application which replicates the users from an LDAP source into a
[GitLab](https://about.gitlab.com) server.

# Presentation

## Context

At État de Genève we use an Active Directory server to authenticate users, plus a home-made solution
(hereafter denoted as "the LDAP server" ) to manage authorizations.
As is done in many organizations, the LDAP server organizes users in groups such
as "IT-DEV-JAVA", "IT-DEV-PHP" or "FINANCE".
Access rights to systems and applications are granted to groups or to users.
A user is assigned to any number of groups.

Now GitLab comes into play.
The server is an on-premise instance of the GitLab community edition.
Our need is to *replicate automatically the LDAP configuration of the users onto the GitLab server*.
For example, we want the GitLab server to acquire automatically groups "IT-DEV-JAVA', 'IT-DEV-PHP', etc.,
from the LDAP server, as well as the members of every group.
We also want the GitLab group members to acquire the adequate right permissions.

# Functional description

This section specifies the expected behavior of the application.

## Definitions

### Standard groups and administrator group

Most LDAP groups are "standard" groups. One group can be an "administrator" group.
For example, LDAP group "IT-DEV-JAVA" is a standard group, whereas LDAP group "***REMOVED***" is the
administrator group.
The name of the administrator group, if any, is supplied as a parameter to the application.

### Wide-access GitLab users

Some special GitLab users require read-only access on all GitLab groups.
These users are usually technical users.
At État de Genève, such a wide-access GitLab user has been created to allow a
[Fisheye](https://www.atlassian.com/software/fisheye) server to log on to GitLab and to freely retrieve commits
from any Git repository.
The list of wide-access GitLab users, if any, is supplied as a parameter to the application.

## Business rules

B1. For every standard LDAP group, create a GitLab group (hereafter coined the "matching group") with the same name,
if such group does not exist yet.
 
B2. For every standard LDAP group, retrieve the list of users (L1).
  * For every user in list L1:
    * If the user does not exist in GitLab, do nothing
      (see section [GitLab authentication](#gitlab-authentication)).
    * **A FAIRE !** If the user exists in GitLab, set its GitLab access level to Regular (as opposed to Admin).
    * If the user exists in GitLab and is not assigned to the matching group, 
      assign it with the "Maintainer" GitLab role.
      This is the main business rule of the application - it is actually its basic purpose.
    * If the user exists in GitLab and is assigned to the matching group, do nothing.
  * Additionally:  
    * If a user exists in GitLab, is assigned to the matching group but is not in list L1, remove it from
      the matching group, unless the user belongs to the LDAP administrator group (see reason below).

B3. For the administrator LDAP group (if any), retrieve the list of users. For every user in the list:
  * If the user does not exist in GitLab, do nothing
    (see section [GitLab authentication](#gitlab-authentication)).
  * If the user exists in GitLab:
    * Give it the Admin (as opposed to Regular) access level.
    * Assign it to all non-administrator groups (with Maintainer role permission), except the groups in a
      black list supplied as a parameter to the application.

B4. For every wide-access GitLab user:
  * Assign it to every group (with Reporter role permission), except the groups in the black list.

Note: the business rules for the standard groups (B2) are applied before the business rules for the administrator
group (B3), otherwise GitLab users having Admin access level would end up being downgraded to Regular access level.

## Remarks

* The application does not create GitLab users.
* The application does not assign other GitLab role permission than Maintainer and Reporter.
  Assignment of a GitLab role permission other than Maintainer, e.g., Developer, Guest or Owner, must be carried out
  directly on the GitLab server. 
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

At État de Genève, users known to the LDAP server but still unknown to the GitLab server acquire their
configuration on the GitLab server by means of the following sequence of operations:
1. On a browser the user navigates to the GitLab server.
   This performs authentication.
   In response the GitLab displays a blank page, which leads the user to believe that connexion went wrong, but
   this weird behavior is expected.
   Behind the scenes GitLab creates a user with access level "Regular" (as opposed to "Admin").
1. Next time the application is run, it loads the user's configuration into GitLab.
1. When the user navigates again to the GitLab server, they readily see all projects they have access to.
   For example, if the user belongs to groups "GROUP-IT-DEV" and "GROUP-IT-NETWORK", they will have access
   to all Git projects defined in these 2 groups as well as in their sub-groups. 

For each user, the hand-shake step 1 above occurs only once. 

# Technical facts

The application is a standalone application. It performs the replication once and then exits.
It requires Java 8+ to build and run.

Communication with the LDAP server is performed by means of a home-made Java library named
[gina-ldap-client](https://github.com/republique-et-canton-de-geneve/gina-ldap-client)
(in French) which internally resorts to the standard `javax.naming.ldap` API.
That library is specifically tuned to the LDAP data model of Gina, the État de Genève's LDAP server.
Transforming the application to connect to another LDAP server than Gina entails the following changes:
* Creating a class, say, `CustomLdapTreeBuilder` that implements interface
  [LdapTreeBuilder](./src/main/java/ch/ge/cti_composant/gitSync/util/ldap/LdapTreeBuilder.java);
* In the top class
  [GitSync](./src/main/java/ch/ge/cti_composant/gitSync/GitSync.java), replacing the usage of
  [GinalLdapTreeBuilder](./src/main/java/ch/ge/cti_composant/gitSync/util/ldap/gina/GinaLdapTreeBuilder.java)
  with that of `CustomLdapTreeBuilder`;
* In the properties file [configuration.properties](./configuration.properties),
  replacing the settings of the Gina LDAP server with those of the custom LDAP server.

Communication with the GitLab server is performed by means of GitLab's own
[java-gitlab-api](https://mvnrepository.com/artifact/org.gitlab/java-gitlab-api)
library, which internally resorts to REST services. 
The application is compatible with any GitLab server, for example GitLab 11, that complies with version 4 of the
GitLab API.

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

# Execution

The mapping is carried out by running the application:

``
java -jar target/gitSync-XXX-SNAPSHOT.jar configuration.properties
``

The unique parameter is the path to the configuration file.
The parameters in the supplied file [configuration.properties](./configuration.properties) must be adapted.

Logging is performed by means of Logback. A basic configuration file `logback.xml` is included in the source
tree and is present in the JAR file created by the `mvn install` command.
It is sufficient for standard analysis, although some people might find the INFO-level console output too verbose.

At État de Genève, execution typically takes a few minutes to execute. It processes
about 100 groups encompassing 1000 group users.

Practical usage requires spawning the application regularly, for example every hour.
This can be done with a crontab-like job.

# Future evolutions

No future evolutions are planned, besides possibly adding unit tests.
