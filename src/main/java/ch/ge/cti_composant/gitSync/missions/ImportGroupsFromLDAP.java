package ch.ge.cti_composant.gitSync.missions;

import ch.ge.cti_composant.gitSync.common.HttpClient;
import gina.api.GinaApiLdapBaseAble;
import gina.api.GinaApiLdapBaseFactory;
import gina.api.GinaException;
import javafx.application.Application;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class ImportGroupsFromLDAP implements Mission {
	Logger log = Logger.getLogger(ImportGroupsFromLDAP.class.getName());
	final private static String ADMIN_LDAP_GROUP = "***REMOVED***";

	/**
	 * Charge les rôles Gina, puis crée les groupes dans GitLab.
	 */
	@Override
	public void start() {
		log.info("Synchronisation : LDAP Groups to GitLab Groups");
		Properties props = new Properties();
		try {
			// Chargement du fichier de properties
			props.load(ImportGroupsFromLDAP.class.getResourceAsStream("/distribution.properties"));
			GinaApiLdapBaseAble api = GinaApiLdapBaseFactory.getInstanceApplication();
			Pattern groupRegexp = Pattern.compile(props.getProperty("ldap.regex.match-group"));
			log.info("Recherche des groupes grâce au regexp suivant : " + props.getProperty("ldap.regex.match-group"));

			// Parse des groupes
			List<String> roles = api.getAppRoles("GESTREPO");

			// Feedback user
			log.info("Récupération des groupes terminée. Groupes détectés : ");
			roles.forEach(role -> log.info(role + (isLDAPGroupAdmin(role) ? " *" : "")));
			log.info("Groupe au total : " + roles.size());
			log.info("(le groupe marqué par * est un groupe admin)");
			log.info("======================================================");

			// Envoi dans GitLab.
			roles.stream()
					.filter(role -> !isLDAPGroupAdmin(role)) // Les admins n'ont pas de groupe sur GitLab.
					.forEach(role -> {
				LinkedList<NameValuePair> params = new LinkedList<>();
				params.add(new BasicNameValuePair("name", role));
				params.add(new BasicNameValuePair("path", role));
				params.add(new BasicNameValuePair("visibility", "public"));
				params.add(new BasicNameValuePair("request_access_enabled", "false"));
				HttpClient http = new HttpClient("***REMOVED***/api/v4/groups", params);
				if (!http.execute()){
					log.warn("Le message semble ne pas s'être envoyé correctement.");
				}
				System.exit(0);
			});
		} catch (GinaException | RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Impossible de trouver le fichier distribution.properties. Arrêt.");
		}
	}

	private static boolean isLDAPGroupAdmin(String role){
		return role.equals(ADMIN_LDAP_GROUP);
	}
}
