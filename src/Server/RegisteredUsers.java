package Server;

import java.util.concurrent.ConcurrentHashMap;
import Exceptions.UserAlreadyExists;
import Exceptions.UserNotFound;

/** Classe che rappresenta la lista di utenti registrati al servizio tramite HashMap di oggetti User.
 * Singleton (instance) garantisce unica istanza di questa classe */

public class RegisteredUsers {
	private static RegisteredUsers instance;
	private ConcurrentHashMap<String,User> registeredUsers;
	
	// costruttore
	private RegisteredUsers () {
		registeredUsers=  JSONUtils.readFromJSON(ConfigurationSettings.FILE_JSON);
	}
	/* EFFECTS: Carica da file JSON tutti gli utenti, e relative informazioni, nell'HashMap */
	
	// (DOUBLE-CHECKED LOCKING) Implementazione Singleton ottimizzata, uso synchronized solo se ==null
	public static RegisteredUsers getInstance () {
		if (instance==null)
			synchronized(RegisteredUsers.class) {
				if (instance==null) instance= new RegisteredUsers();
		}
		return instance;
	}
	
	/* Restituisce l'HashMap di utenti registrati */
	public ConcurrentHashMap<String,User> getRegisteredUsers () {
		return registeredUsers;
	}
	
	/* Restituisce l'oggetto User */
	public synchronized User getUser (String UserName) {
		if (UserName==null || UserName.equals("")) throw new IllegalArgumentException();
		if (registeredUsers.isEmpty()) throw new UserNotFound("User not found");
		return registeredUsers.get(UserName);
	}
	
	/* Restituisce true/false se registeredUsers contiene UserName */
	public synchronized boolean containsUser (String UserName) throws IllegalArgumentException {
		if (UserName==null || UserName.endsWith("")) throw new IllegalArgumentException();
		return registeredUsers.containsKey(UserName);
	}
	
	/* Aggiunge nuovo utente */
	public User addUser (User user) {
		if (user==null) throw new IllegalArgumentException();
		if (registeredUsers.get(user.getUserName())!=null) throw new UserAlreadyExists("User already exists");
		return registeredUsers.putIfAbsent(user.getUserName(), user);
	}
	
	/* Restituisce il boolean stato loggato di user */
	public synchronized boolean isLogged (String UserName) {
		if (UserName==null) throw new IllegalArgumentException();
		if (registeredUsers.isEmpty() || getUser(UserName)==null) throw new UserNotFound("User not found");
		return registeredUsers.get(UserName).isLogged();
	}
	
	/* Imposta il boolean stato loggato di user a 'bool' */
	public synchronized void setLogged (String UserName, boolean bool) {
		if (UserName==null) throw new IllegalArgumentException();
		if (registeredUsers.isEmpty() || getUser(UserName)==null ) throw new UserNotFound("User not found");
		registeredUsers.get(UserName).setLogged(bool);
		
		User u= registeredUsers.get(UserName);
		u.setLogged(bool);
		registeredUsers.replace(UserName, u);
		
	}
}
