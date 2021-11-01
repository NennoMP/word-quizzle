package Server;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

import Exceptions.UserAlreadyExists;

/** Classe implementazione dell'interfaccia che rappresenta il servizio di registrazione.
 * Permette la registrazione di un nuovo utente, se non presente, e ne effettua il salvataggio su file JSON users*/

public class RegistrationService extends RemoteServer implements IRegistrationService {
	public static final long serialVersionUID= 1L;
	private RegisteredUsers registeredUsers;
	
	// costruttore
	public RegistrationService () {
		registeredUsers= RegisteredUsers.getInstance();
	}
	
	/* Restituisce boolean true/false se registra nuovo utente */
	public boolean registra_utente (String UserName, String password) throws RemoteException {
		if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal Username");
		if (password==null || password.equals("")) throw new IllegalArgumentException("Illegal password");
		
		User u= new User(UserName,password);
		if (this.registeredUsers.addUser(u)!=null) throw new UserAlreadyExists("Username already taken");
		JSONUtils.SaveJsonFile();
		return true;
	}
	/* EFFECTS: Registra nuovo utente se non già presente (username) e salva su file */
}
