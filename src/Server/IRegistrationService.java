package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import Exceptions.UserAlreadyExists;

/** Interfaccia del servizio di registrazione **/

public interface IRegistrationService extends Remote {
	/* Restituisce true/false se registra nuovo utente e salva i dati su file JSON users */
	public boolean registra_utente (String UserName, String password) throws RemoteException, IllegalArgumentException, UserAlreadyExists;
}
