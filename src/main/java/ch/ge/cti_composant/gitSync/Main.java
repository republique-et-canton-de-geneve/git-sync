package ch.ge.cti_composant.gitSync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to spawn a {@link GitSync}.
 */
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args){
		if (args.length != 1) {
			LOGGER.error("1 argument expected: the path to configuration file, such as configuration.properties");
		} else {
			new GitSync().run(args[0]);
			LOGGER.info("GitSync completed");
		}
	}

}
