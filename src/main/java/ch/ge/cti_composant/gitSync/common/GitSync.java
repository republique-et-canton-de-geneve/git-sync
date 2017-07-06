package ch.ge.cti_composant.gitSync.common;

import ch.ge.cti_composant.gitSync.missions.ImportGroupsFromLDAP;
import org.apache.log4j.Logger;


/**
 * Main class that does the chit chat between all classes, basically
 */
public class GitSync {
	private Logger log = Logger.getLogger(GitSync.class.getName());

	public void run() {
		// Imports groups from LDAP
		new ImportGroupsFromLDAP().start();

	}

}
