package ch.ge.cti_composant.gitSync.util.gitlab

import ch.ge.cti_composant.gitSync.data.DataProvider
import org.gitlab.api.models.GitlabGroup
import org.gitlab.api.models.GitlabUser
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link GitLabSpec}.
 */
@Unroll
class GitLabSpec extends Specification {

	def "#getGroups should return the expected GitLab groups"() {
		given:
		def gitlab = new DataProvider().setupGitlabTree()

		when:
		List<GitlabGroup> groups = gitlab.getGroups().sort{g1, g2 -> g1.getName().compareTo(g2.getName())}

		then:
		groups.size() == 2
		groups.get(0).getName() == "Dev"
		groups.get(1).getName() == "Network"
	}

	def "#getUsers(group) should return the expected GitLab users"() {
        given:
        def gitlab = new DataProvider().setupGitlabTree()
        List<GitlabGroup> groups = gitlab.getGroups().sort{g1, g2 -> g1.getName().compareTo(g2.getName())}
        def devGroup = groups.get(0)

        when:
        Map<String, GitlabUser> users = gitlab.getUsers(devGroup)

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
        users.get("Marie").getName() == "Marie"
    }

	def "#getUsers should return all GitLab users"() {
        given:
        def gitlab = new DataProvider().setupGitlabTree()

        when:
        Map<String, GitlabUser> users = gitlab.getUsers()

        then:
        users.size() == 3
        users.containsKey("Jean")
        users.containsKey("Marie")
        users.containsKey("Paul")
        users.get("Marie").getName() == "Marie"
    }

}
