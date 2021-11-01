package Server;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import Exceptions.FriendAlreadyExists;
import com.fasterxml.jackson.annotation.*;

/** Classe oggetto che rappresenta un utente del servizio insieme a tutte le sue informazioni personali (username, password, score, lista amici), 
 * a queste si aggiungono quelle necessarie per la gestione delle interazioni con il server (stato loggato e in gioco, porta UDP e SelectionKey).
 * Queste ultime sono contrassegnate con @JsonIgnore poichè è inutile che vengano serializzate nel JSON file.
 * Viene garantita mutua esclusività per il la variabile 'playing' e per le modifiche alla lista amici rispettivamente tramite l'utilizzo di AtomicBoolean e metodi synchronized,
 * questo per via di possibili accessi da parte di thread differenti.
 **/

public class User {
	// user-info
	private long scoreTOT;
	private String UserName;
	private String password;
	private ConcurrentHashMap<String,String> friendsList;

	// JsonIgnore-info
	@JsonIgnore
	private int UDPport;
	@JsonIgnore
	private SelectionKey selKey;
	@JsonIgnore
	private boolean logged;
	@JsonIgnore
	private AtomicBoolean playing;

	// costruttore
	public User() {
		this.UDPport = ConfigurationSettings.UDP_PORT;
		this.logged = false;
		this.playing = new AtomicBoolean(false);
	}

	// costruttore (CLIENT)
	/** Costruttore utilizzato dal client **/
	public User(String UserName, String password) {
		this.UserName = UserName;
		this.password = password;
		this.playing = new AtomicBoolean(false);
		friendsList = new ConcurrentHashMap<String,String>();
	}

	// costruttore (REGISTRATION-RMI)
	/** Costruttore per fase di registrazione **/
	public User(SelectionKey selKey) {
		this.UDPport = ConfigurationSettings.UDP_PORT;
		this.selKey = selKey;
		this.playing = new AtomicBoolean(false);
	}

	// USER INFO METHODS
	
	/* Restituisce il punteggio dell'utente */
	public synchronized long getTotalScore() {
		return this.scoreTOT;
	}

	/* Restituisce l'username dell'utente */
	public String getUserName() {
		return UserName;
	}

	/* Restituisce la password dell'utente */
	public String getPassword() {
		return password;
	}

	// JSON-IGNORE INFO METHODS
	
	/* Restituisce la porta UDP dell'utente */
	public int getUDP() {
		return UDPport;
	}

	/* Imposta la porta UDP dell'utente */
	public void setUDP(int port) {
		this.UDPport = port;
	}

	/* Restituisce la SelectionKey dell'utente */
	public SelectionKey getKey() {
		return selKey;
	}

	/* Imposta la SelectionKey dell'utente */
	public void setKey(SelectionKey selKey) {
		this.selKey = selKey;
	}
	
	/* Restituisce il boolean per lo stato loggato dell'utente */
	public boolean isLogged() {
		return logged;
	}

	/* Imposta il boolean stato loggato utente a 'bool' */
	public void setLogged(boolean bool) {
		this.logged = bool;
	}
	
	/* Restituisce il boolean per lo stato in gioco dell'utente */
	public boolean isPlaying() {
		if (this.playing == null) {
			this.playing = new AtomicBoolean(false);
		}
		return this.playing.get();
	}

	/* Imposta il boolean stato in gioco utente a 'bool' */
	public void setPlaying(boolean bool) {
		this.playing.set(bool);
	}

	// FRIENDS LIST METHODS (SYNCHRONIZEDS)
	
	/* Aggiunge un amico alla lista amici dell'utente */
	public void addFriend (String friend) throws IllegalArgumentException, FriendAlreadyExists  {
		if (friend==null || friend.equals("")) throw new IllegalArgumentException("Illegal friend name");
		if (friendsList.get(friend)!=null) throw new FriendAlreadyExists("Friend already added");
		friendsList.putIfAbsent(friend, friend);
	}

	/* Restituisce true/false se l'amico appartiene alla lista amici dell'utente */
	public synchronized boolean isFriend(String friend) throws IllegalArgumentException {
		if (friend == null || friend.equals(""))
			throw new IllegalArgumentException("Illegal friend name");
		return (friendsList.get(friend)!=null);
	}
	
	/* Restituisce la lista amici dell'utente (HashMap) */
	public ConcurrentHashMap<String,String> getFriends () {
		if (this.friendsList==null) this.friendsList= new ConcurrentHashMap<>();
		return friendsList;
	}

	// USER-SCORE METHODS
	
	/* Aggiorna il punteggio dell'utente */
	public synchronized void UpdateScore(long score) {
			this.scoreTOT+= score;
	}
	
	/* Imposta il punteggio dell'utente */
	public synchronized void setTotalScore (long score) {
		this.scoreTOT= score;
	}

}