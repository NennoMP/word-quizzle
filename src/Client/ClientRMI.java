package Client;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Exceptions.UserAlreadyExists;
import Exceptions.WrongPassword;
import Server.ConfigurationSettings;
import Server.IRegistrationService;

/** Client RMI per registrare nuovo utente */

public class ClientRMI {
    public boolean registra_utente(String username, String pw) {
        Remote remoteObject;
        IRegistrationService serverObject;

        try {
        	// registry & lookup
            Registry r = LocateRegistry.getRegistry(ConfigurationSettings.RMI_PORT);
            remoteObject = r.lookup("REGISTRATION-SERVICE"); 
            serverObject = (IRegistrationService) remoteObject;
            
            return serverObject.registra_utente(username, pw);
        } catch (IllegalArgumentException | UserAlreadyExists | WrongPassword e) {
            System.out.println(e.getMessage());
        } catch (RemoteException | NotBoundException e2) {
            e2.printStackTrace();
        }
        return false;
    }
}