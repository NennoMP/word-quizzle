package Server;

import java.util.concurrent.ConcurrentHashMap;


import Exceptions.ChallengeAlreadyExists;
import Exceptions.ChallengeNotFound;

/** Classe che rappresenta la lista di sfide in esecuzione (oggetti Challenge).
 * Singleton pattern (instance) garantisce unica istanza della classe
 */

public class ChallengesList {
    private static ChallengesList instance;
    private ConcurrentHashMap<Integer, Challenge> challengesList;

    // costruttore
    private ChallengesList() {
        challengesList = new ConcurrentHashMap<>();
    }

    // (DOUBLE-CHECKED LOCKING) Implementazione Singleton ottimizzata, uso synchronized solo se ==null
    public static synchronized ChallengesList getInstance() {
    	if (instance==null)
    		synchronized (ChallengesList.class) {
    			if (instance==null) instance= new ChallengesList();
    		}
    	return instance;
    }
    
    /* Restituisce l'HashMap conentente le sfide */
    public ConcurrentHashMap<Integer, Challenge> getHashListaSfide() {
        return challengesList;
    }

    /* Aggiunge la sfida */
    public synchronized Challenge addChallenge (Challenge challenge) {
        if (challenge==null) throw new IllegalArgumentException();
        if (!challengesList.isEmpty() && challengesList.get(challenge.getID()) != null) throw new ChallengeAlreadyExists("Challenge already exists");
        return challengesList.putIfAbsent(challenge.getID(), challenge);
    }

    /* Rimuove la sfida */
    public synchronized void removeChallenge (Challenge challenge) {
        if (challenge==null) throw new IllegalArgumentException();
        if (challengesList.isEmpty()) throw new ChallengeNotFound("Challenge not found");
        challengesList.remove(challenge);
    }
}
