package ch.ge.cti_composant.gitSync.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will load everything in place.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args){
		if (args.length != 1) {
			LOGGER.error("Maximum un argument (arg1 : path distribution.properties)");
		} else {
			LOGGER.info("Démarrage de l'utilitaire de synchronisation LDAP <=> Git.");
			new GitSync().run(args[0]); // Boot
			LOGGER.info("Synchronisation terminée. Bonne nuit...");
		}
	}

}
