package ch.ge.cti_composant.gitsync.util.gitlab

import ch.ge.cti_composant.gitsync.util.exception.GitSyncException
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.GroupApi
import org.gitlab4j.api.UserApi
import org.gitlab4j.api.models.AccessLevel
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.GroupParams
import org.gitlab4j.api.models.Member
import org.gitlab4j.api.models.User
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests class {@link GitlabAPIWrapper}.
 */
@Unroll
class GitlabAPIWrapperSpec extends Specification {
    def "getGroups"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def groupApi = Mock(GroupApi)
        api.getGroupApi() >> groupApi
        groupApi.getGroups() >> [new Group().withName("groupe1"), new Group().withName("groupe2")]

        when:
        def groups = gitlabAPIWrapper.getGroups()

        then:
        groups != null
        groups.size() == 2
    }

    def "getGroup"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def groupApi = Mock(GroupApi)
        api.getGroupApi() >> groupApi
        groupApi.getGroup("group1") >> new Group().withName("group1")

        when:
        def group = gitlabAPIWrapper.getGroup("group1")

        then:
        group != null
        group.name == "group1"
    }

    def "createGroup"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def groupApi = Mock(GroupApi)
        def groupParams = new GroupParams()
        groupParams.withName("group1")

        api.getGroupApi() >> groupApi
        groupApi.createGroup(groupParams) >> new Group().withName("group1")

        when:
        def group = gitlabAPIWrapper.createGroup(groupParams)

        then:
        group != null
        group.getName() == "group1"
    }

    def "getGroupMembers"() {
        given:
        def api = Mock(GitLabApi)
        def groupApi = Mock(GroupApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def group = new Group().withName("group1").withId(1)

        api.getGroupApi() >> groupApi
        groupApi.getMembers(1) >> [new Group().withName("member1"), new Group().withName("member2")]

        when:
        def members = gitlabAPIWrapper.getGroupMembers(group)

        then:
        members != null
        members.size() == 2
    }

    def "addGroupMember"() {
        given:
        def api = Mock(GitLabApi)
        def groupApi = Mock(GroupApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def group = new Group().withName("group1").withId(1)

        api.getGroupApi() >> groupApi
        groupApi.addMember(group, 2, AccessLevel.DEVELOPER) >> new Member().withName("member1")

        when:
        def member = gitlabAPIWrapper.addGroupMember(group, 2, AccessLevel.DEVELOPER)

        then:
        member != null
        member.getName() == "member1"
    }

    def "deleteGroupMember"() {
        given:
        def api = Mock(GitLabApi)
        def groupApi = Mock(GroupApi)
        api.getGroupApi() >> groupApi

        when:
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def group = new Group().withName("group1").withId(1)

        gitlabAPIWrapper.deleteGroupMember(group, 2)

        then:
        1 * groupApi.removeMember(1, 2)
    }

    def "getUser"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def userApi = Mock(UserApi)

        api.getUserApi() >> userApi
        userApi.getCurrentUser() >> new User().withName("self")

        when:
        def user = gitlabAPIWrapper.getUser()

        then:
        user != null
        user.getName() == "self"
    }

    def "getUsers"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def userApi = Mock(UserApi)

        api.getUserApi() >> userApi
        userApi.getUsers() >> [new User().withName("user1"), new User().withName("user2")]

        when:
        def users = gitlabAPIWrapper.getUsers()

        then:
        users != null
        users.size() == 2
    }

    def "promoteToAdmin"() {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def userApi = Mock(UserApi)
        def user = Mock(User)

        api.getUserApi() >> userApi
        userApi.getUser(1) >> user

        when:
        gitlabAPIWrapper.promoteToAdmin(1)

        then:
        1 * user.setIsAdmin(true)
        1 * userApi.updateUser(user, null)
    }

    def "blockUser" () {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def userApi = Mock(UserApi)

        api.getUserApi() >> userApi

        when:
        gitlabAPIWrapper.blockUser(1)

        then:
        1 * userApi.blockUser(1)
    }

    def "unblockUser" () {
        given:
        def api = Mock(GitLabApi)
        def gitlabAPIWrapper = new GitlabAPIWrapper(api)
        def userApi = Mock(UserApi)

        api.getUserApi() >> userApi

        when:
        gitlabAPIWrapper.unblockUser(1)

        then:
        1 * userApi.unblockUser(1)
    }

    def "getUsers should retry up to 3 times and succeed"() {
        given:
        def api = Mock(GitLabApi)
        def userApi = Mock(UserApi)
        def wrapper = new GitlabAPIWrapper(api)

        api.getUserApi() >> userApi

        1 * userApi.getUsers() >> { throw new GitLabApiException("fail 1") }
        1 * userApi.getUsers() >> { throw new GitLabApiException("fail 2") }
        1 * userApi.getUsers() >> [new User(name: "user1"), new User(name: "user2")]

        when:
        def users = wrapper.getUsers()

        then:
        users.size() == 2
        users*.name == ["user1", "user2"]
    }

    def "getUsers should throw GitSyncException after max retries"() {
        given:
        def api = Mock(GitLabApi)
        def userApi = Mock(UserApi)
        def wrapper = new GitlabAPIWrapper(api)

        api.getUserApi() >> userApi
        3 * userApi.getUsers() >> { throw new GitLabApiException("fail always") }

        when:
        wrapper.getUsers()

        then:
        thrown(GitSyncException)
    }
}
