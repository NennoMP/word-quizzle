package Server;


/** Mainclass del WordQuizzle server 
 * @author Pinna Matteo
 * */


public class ServerMain {

	public static void main(String[] args) {
		// preparo RMI
		ServerRMI registration= new ServerRMI();
		registration.start();
		
		// inizializzo server
		Server server= new Server();
		
		// start
		try {
			server.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
