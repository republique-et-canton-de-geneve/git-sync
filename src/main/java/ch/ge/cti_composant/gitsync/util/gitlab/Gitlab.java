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
package ch.ge.cti_composant.gitsync.util.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The GitLab context.
 * Consists mainly in the GitLab tree (groups and members), usually restricted to the elements that come from
 * the LDAP server.
 */
public class Gitlab {

	private final GitlabAPIWrapper api;

	private final Set<Group> groups;

	public Gitlab(Set<Group> groups, String url, String apiKey) {
		Objects.requireNonNull(groups);
		this.groups = groups.stream()
				.sorted(Comparator.comparing(Group::getName))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		this.api = new GitlabAPIWrapper(new GitLabApi(url, apiKey));
	}

	/**
	 * Returns the stub to the GitLab server.
	 */
	public GitlabAPIWrapper getApi() {
		return api;
	}

	/**
	 * Returns all GitLab groups, sorted by names.
	 */
	public Set<Group> getGroups() {
		return groups;
	}

}
