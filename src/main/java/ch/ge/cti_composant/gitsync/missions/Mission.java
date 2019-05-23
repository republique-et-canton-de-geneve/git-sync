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
package ch.ge.cti_composant.gitsync.missions;

import ch.ge.cti_composant.gitsync.util.ldap.LdapTree;
import ch.ge.cti_composant.gitsync.util.gitlab.Gitlab;

/**
 * A mission is an operation performed on the GitLab server, using the LDAP tree.
 */
public interface Mission {

	void start(LdapTree ldapTree, Gitlab gitlab);

}
