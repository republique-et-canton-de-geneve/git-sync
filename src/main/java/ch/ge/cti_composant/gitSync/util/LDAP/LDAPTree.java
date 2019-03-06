package ch.ge.cti_composant.gitSync.util.LDAP;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ge.cti_composant.gitSync.common.GitSync;
import gina.api.GinaApiLdapBaseAble;
import gina.impl.GinaLdapFactory;
import gina.impl.util.GinaLdapConfiguration;

/**
 * Represente l'arbre des utilisateurs LDAP.
 */
public class LDAPTree {

	private static final Logger LOGGER = LoggerFactory.getLogger(LDAPTree.class);

	/**
	 * Carte.
	 */
	private Map<LDAPGroup, Map<String, LDAPUser>> tree;

	/**
	 * Attributs LDAP a recuperer.
	 */
	private String[] attributes = {"cn"};

	public LDAPTree() throws IOException {
		tree = new HashMap<>();
		build();
	}

	/**
	 * Remplit l'arbre LDAP de groupes, puis d'utilisateurs.
	 */
	private void build() throws IOException {
		tree = new HashMap<>();
		GinaLdapConfiguration conf = new GinaLdapConfiguration(
				GitSync.getProperty("ct-gina-ldap-client.LDAP_SERVER_URL"),
			    GitSync.getProperty("ct-gina-ldap-client.LDAP_BASE_DN"),
			    GitSync.getProperty("ct-gina-ldap-client.LDAP_USER"),
			    GitSync.getProperty("ct-gina-ldap-client.LDAP_PASSWORD"),
			    GinaLdapConfiguration.Type.APPLICATION,
			    Integer.valueOf(GitSync.getProperty("timeout-search-ldap")));
		GinaApiLdapBaseAble app = GinaLdapFactory.getInstance(conf);
		
		try {
			app.getAppRoles("GESTREPO").forEach(role -> tree.put(new LDAPGroup(role), new HashMap<>()));
			tree.forEach((ldapGroup, ldapUsers) -> {
				LOGGER.info("Recuperation des utilisateurs pour le groupe LDAP [{}]", ldapGroup.getName());
				try {
					app.getUsers("GESTREPO", ldapGroup.getName(), attributes)
							.forEach(user -> {
								if (user.containsKey("cn")) {
									LOGGER.info("\t{}", user.get("cn"));
									ldapUsers.put(user.get("cn"), new LDAPUser(new HashMap<>(user)));
								}
							});
				} catch (RemoteException e) {
					LOGGER.error("J'eprouve certaines difficultes a me connecter au vLDAP : {}", e);
				}
			});
		} catch (IOException e) {
			LOGGER.error("Impossible de lancer la recherche LDAP car le fichier distribution.properties est introuvable : {}", e);
			throw e;
		}
	}

	public List<LDAPGroup> getGroups(){
		return new ArrayList<>(tree.keySet());
	}

	public Map<String, LDAPUser> getUsers(LDAPGroup group){
		return new HashMap<>(tree.getOrDefault(group, new HashMap<>()));
	}

	public Map<String, LDAPUser> getUsers(String group){
		return getUsers(new LDAPGroup(group));
	}

}
