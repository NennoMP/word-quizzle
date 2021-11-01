package Client;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;

/** Classe utile per sincronizzazione console e server UDP, in modo da prendere in input
 * la risposta alla sfida
 */

public class ChallengeRequest {
	private static ChallengeRequest instance;
	
	private Timestamp timeout;
	public AtomicBoolean challengeToAnswer;
	
	// costruttore
	public ChallengeRequest () {
		challengeToAnswer= new AtomicBoolean(false);
	}
	
	// (DOUBLE-CHECKED LOCKING) Implementazione Singleton ottimizzata, uso synchronized solo se ==null
	public static synchronized ChallengeRequest getInstance () {
		if (instance==null)
			synchronized (ChallengeRequest.class) {
				if (instance==null) instance= new ChallengeRequest();
			}
		return instance;
	}
	
	/* Restituisce true/false se devo rispondere a richieste sfida */
	public AtomicBoolean getToAnswer () {
		return challengeToAnswer;
	}
	
	/* Imposta boolean a 'bool' se devo risponde a richieste sfida */
	public void setToAnswer (AtomicBoolean toAnswer) {
		this.challengeToAnswer= toAnswer;
	}
	
	/* Imposta Timestamp di timeout */
	public synchronized void setTimeout (Timestamp timeout) {
		this.timeout= timeout;
	}
	
	/* Restituisce Timestamp di timeout */
	public synchronized Timestamp getTimeout () {
		return timeout;
	}

}
