package ch.ge.cti_composant.gitSync.util.LDAP;

import gina.api.GinaApiLdapBaseAble;
import gina.api.GinaApiLdapBaseFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represente l'arbre des utilisateurs LDAP.
 */
public class LDAPTree {
	// Logger
	Logger log = Logger.getLogger(LDAPTree.class.getName());
	// Carte
	private Map<LDAPGroup, Map<String, LDAPUser>> ldapTree;
	// Attributs a recuperer LDAP
	private String[] attributes = {"cn"};

	public LDAPTree() throws IOException {
		ldapTree = new HashMap<>();
		build();
	}

	/**
	 * Remplit l'arbre LDAP de groupes, puis d'utilisateurs.
	 */
	public void build() throws IOException {
		this.ldapTree = new HashMap<>();
		GinaApiLdapBaseAble app = GinaApiLdapBaseFactory.getInstanceApplication();
		try {
			app.getAppRoles("GESTREPO").forEach(role -> ldapTree.put(new LDAPGroup(role), new HashMap<>()));
			ldapTree.forEach((ldapGroup, ldapUsers) -> {
				log.debug("R�cup�ration des utilisateurs pour le groupe LDAP " + ldapGroup.getName());
				try {
					app.getUsers("GESTREPO", ldapGroup.getName(), attributes).forEach(user -> {
					    	if (user.containsKey("cn"))
					    	{
					    	    ldapUsers.put(user.get("cn"), new LDAPUser(new HashMap<>(user)));
					    	}
					});
				} catch (RemoteException e) {
					log.error("J'�prouve certaines difficult�s �me connecter au vLDAP : " + e);
				}
			});
		} catch (IOException e) {
			log.error("Impossible de lancer la recherche LDAP car le fichier distribution.properties est introuvable. " + e);
			throw e;
		}
	}

	public List<LDAPGroup> getGroups(){
		return new ArrayList<>(ldapTree.keySet());
	}

	public Map<String, LDAPUser> getUsers(LDAPGroup group){
		return new HashMap<>(ldapTree.getOrDefault(group, new HashMap<>()));
	}

	public Map<String, LDAPUser> getUsers(String group){
		return getUsers(new LDAPGroup(group));
	}


}
