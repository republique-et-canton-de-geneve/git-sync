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
			LOGGER.error("1 argument expected: the path to property file, such as distribution.properties");
		} else {
			LOGGER.info("Start GitSync");
			new GitSync().run(args[0]); // Boot
			LOGGER.info("GitSync mapping completed");
		}
	}

}
