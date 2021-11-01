package Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/** Server RMI per registrare un nuovo utente **/

public class ServerRMI {
	
	public void start() {
		try {
			// stub
			RegistrationService reg= new RegistrationService();
			IRegistrationService stub= (IRegistrationService) UnicastRemoteObject.exportObject(reg, ConfigurationSettings.RMI_PORT);
			
			// registry & pubblicazione
			LocateRegistry.createRegistry(ConfigurationSettings.RMI_PORT);
			Registry r= LocateRegistry.getRegistry(ConfigurationSettings.RMI_PORT);
			r.rebind("REGISTRATION-SERVICE", stub);
			
			System.out.println("RMI ready!");
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
