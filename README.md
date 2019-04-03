gitSync is a standalone Java application which replicates the users from an LDAP source into a
[GitLab](https://about.gitlab.com) server.

# Presentation

## Context

At État de Genève we use an Active Directory server to authenticate users, plus a home-made solution
(hereafter denoted as "the LDAP server" ) to manage authorizations.
As is done in many organizations, the LDAP server organizes users in roles such
as "IT-DEV-JAVA", "IT-DEV-PHP" or "FINANCE".
Access rights to systems and applications are granted to roles or to users.
A user is assigned any number of roles.

Now GitLab comes into play.
The server is an on-premise instance of the GitLAb community edition.
We want to replicate automatically the configuration of the users in the GitLab server.
For example, we want the GitLab server to acquire automatically groups "IT-DEV-JAVA', 'IT-DEV-PHP', etc.,
from the LDAP server, as well as the names of the users defined in every group. 

# Functional description

This section specifies the expected behavior of the application.

## Definitions

Most LDAP roles are "standard" roles. One role can be an "administrator" role.
For example, role "IT-DEV-JAVA" is a standard role, whereas "ADMIN" is the administrator role.
The administrator role, if any, is supplied as a parameter to the application.

~~(in the GitLab server) Most groups are "standard" groups. A few groups are "administrator" groups.
For example, group "IT-DEV-JAVA" is a standard group, whereas groups "IT-ADMIN" and "FINANCE-ADMIN" are administrator
groups.
Standard groups are typically created by the application, whereas administrator groups are created directly
(manually) on the GitLab server.
The list of administrator groups is supplied to the application.
Administrator roles (in the LDAP server) and administrator groups (in the GitLab server) do not need to be correlated.~~

## Business rules

* For every standard LDAP role, create a GitLab group (hereafter coined the "matching group") with the same name, 
if such group does not exist yet.
 
* For every standard LDAP role, retrieve the list of users. 
  * For every user in the list:
    * If the user does not exist in GitLab, do nothing (see section ``GitLab authentication`` below).
    * **A FAIRE !** If the user exists in GitLab, set its GitLab access level to Regular (as opposed to Admin).
    * If the user exists in GitLab and is not assigned to the matching group, 
      assign it with the "Maintainer" GitLab role.
    * If the user exists in GitLab and is assigned to the matching group, do nothing.

  * Additionally:  
    * If a user exists in GitLab, is assigned to the matching group but is not in the list above, remove it from
      the matching group, unless the user belongs to the LDAP administrator role (see reason below).
  
* For the administrator LDAP role (if any), retrieve the list of users. For every user in the list:
  * If the user does not exist in GitLab, do nothing (see section ``GitLab authentication`` below).
  * If the user exists in GitLab :
    * Assign it the Admin (as opposed to Regular) access level.
    * Assign it to all non-administrator groups (with Maintainer GitLab role), except the groups in a black list
      supplied as a parameter to the application.

Note: the business rules for standard roles are applied before the business rules for the administrator role,
otherwise users having Admin access level are downgraded to Regular access level.

## Remarks

* The application does not create GitLab users.
* The application does not assign other GitLab role permission than "Maintainer".
  Assignment of a GitLab role permission other than Maintainer, e.g., Developer, Guest or Owner, is carried out
  directly on the GitLab server. 
* The application does not create GitLab sub-groups. It only creates GitLab groups.
* Whenever the application creates a GitLab group, the GitLab role permission "Owner" group is automatically
  assigned by GitLab to the user used for the connection.
  That user is the user matching the connection token ``gitlab.account.token`` to be supplied in the
  parameter file ``distribution.properties``.
* If an existing GitLab group matches no LDAP role, nothing happens to it.
* The replication is one-way : from the LDAP server to the GitLab server. Accordingly, read-only access
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
which internally resorts to the standard `javax.naming.ldap` API.
Transforming the application to connect it to another LDAP server than the one of État de Genève would entail
replacing library gina-ldap-client-impl with a custom library implementing the interfaces defined
in gina-ldap-client-api.
** A FAIRE !!! **

Communication with the GitLab server is performed by means of GitLab's
[java-gitlab-api](https://mvnrepository.com/artifact/org.gitlab/java-gitlab-api)
library, which internally resorts to REST services. 
The application is compatible with any GitLab server, for example GitLab 11, that complies with version 4 of the
GitLab API.

The application stores the whole LDAP configuration of the roles in memory. For a few hundred users this has not
caused any trouble so far.

If some glitch happens when extracting the user configuration from the LDAP server (for example, the data have
been moved to another zone of the LDAP tree) and the application comes up with an empty configuration, replication
to the GitLab server will simply clean up the user assignments, thereby preventing all users from
accessing their projects.
In order to prevent such bad situation from occurring, an extra configuration parameter (**NOM ??**) is provided: 
if the configuration contains fewer users than the value of the parameter, the application exits without performing
the replication. **LOG ? EXCEPTION ?**

# Build

Lancer la commande

``mvn clean install``

# Execution

The synchronization is carried out by running the application:

``
java -jar target/gitSync-XXX-SNAPSHOT.jar ./src/main/resources/distribution.properties
``

At État de Genève, execution typically takes a few minutes to execute. It processes
about 100 groups encompassing 1000 group users.

Practical usage requires spawning the application regularly, for example every hour.

(A FAIRE : paramétrisation)

# Future evolutions

No future evolutions are planed, besides possibly adding unit tests.
