package ch.ge.cti_composant.gitSync.common;

import org.apache.commons.lang.Validate;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Cette classe permet de contacter une server et vérifie qu'il envoie une réponse.
 *
 * @author pirklt (original @author larochep. Thanks pierre !)
 */

public class HttpClient {
	private Logger log = Logger.getLogger(HttpClient.class.getName());

	private static final int TIMEOUT = 1500; // 1.5 secondes
	private String url;
	private List<NameValuePair> parameters;

	/**
	 * Constructeur.
	 *
	 * @param url l'URL à pinger.
	 */

	public HttpClient(String url) {
		Validate.notNull(url);
		this.url = url.trim();
	}

	/**
	 * Constructeur.
	 *
	 * @param URL      l'URL à pinger.
	 */

	public HttpClient(String URL, LinkedList<NameValuePair> parameters) {
		this(URL);
		this.parameters = new LinkedList<>(parameters);
	}


	/**
	 * Exécute la requête.
	 * @return bool Vrai si la requête s'est bien passée, sinon faux.
	 */
	public boolean execute() {
		try {
			// Construction de l'executor avec ou sans authentification.
			Executor executor = Executor.newInstance();
			// Building request
			Request request = Request.Post(this.url + "?" + URLEncodedUtils.format(parameters, "utf-8"));
			Properties p = new Properties();
			p.load(HttpClient.class.getResourceAsStream("/distribution.properties"));
			request.addHeader("PRIVATE-TOKEN", p.getProperty("gitlab.account.token"));
			request.connectTimeout(TIMEOUT);

			Response response = executor.execute(request);
			StatusLine statusLine = response.returnResponse().getStatusLine();
			int code = statusLine.getStatusCode();
			if (code / 100 == 2){
				// Okay, good messages
				log.info("Requête " + request.toString() + " exécutée avec succès.");
				return true;
			} else {
				// Generally speaking, a bad sign.
				log.error("Code inhabituel. J'ai recu un code " + code + " à la place d'un code 2XX.");
			}
		} catch (IOException e) {
			log.error("Impossible de contacter le serveur à l'adresse suivante : " + this.url + ". Raison : " + e);
		}
		return false;
	}


}
