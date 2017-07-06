package ch.ge.cti_composant.gitSync.common;

import org.apache.log4j.Logger;

/**
 * This class will load everything in place.
 */
public class Main {
	private static final Logger log = Logger.getLogger(Main.class);

	public static void main(String[] args){
		log.info("Démarrage de l'utilitaire de synchronisation LDAP <=> Git.");
		new GitSync().run(); // Boot
		log.info("Synchronisation terminée. Bonne nuit...");
	}
}
