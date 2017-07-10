package ch.ge.cti_composant.gitSync.util;

import java.io.IOException;

/**
 * Représente une instance d'annuaire
 */
public interface AnnuaireTree {
	/**
	 * Remplit l'arbre visant une vue de l'annuaire
	 */
	void build() throws IOException;

}
