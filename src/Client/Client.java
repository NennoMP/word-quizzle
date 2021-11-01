package Client;
import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import Exceptions.AlreadyLoggedIn;
import Server.ConfigurationSettings;
import Server.User;
import Server.JSONUtils;
import Server.TimestampUtils;

/** Mainclass del WordQuizzle Client.
 * Si occupa di fate il parsing dei comandi da console ed inoltrarli, insieme a relative informazioni (username, password) se presenti,
 * al Server e allo stesso tempo di ricevere messaggi di conferma o errore e stamparli all'utente
 * @author Pinna Matteo
 * */

public class Client {
	static User currUser= null;
	static SocketChannel client;
	static final BufferedReader consoleReader= new BufferedReader(new InputStreamReader(System.in));
	
	static ChallengeRequest challengeRequest= ChallengeRequest.getInstance();
	static ListenerUDP listenerUDP;
	static int portUDP= ConfigurationSettings.UDP_PORT;
	
	static boolean playing= false;
	static boolean exit= false;

	/* Se non ho porta UDP da linea di comando considero quella di default */
	public static void main(String[] args) {
		portUDP= ConfigurationSettings.UDP_PORT;
		if (args.length>0) {
			portUDP= Integer.parseInt(args[0]);
		}
		// Se non rispetta i requisiti la imposto a default
		if (portUDP < ConfigurationSettings.UDP_PORT) portUDP= ConfigurationSettings.UDP_PORT;
		
		// faccio partire il listener per le richieste sfida
		listenerUDP= new ListenerUDP(portUDP);
		Thread thread= new Thread(listenerUDP);
		thread.start();
		
		// apro connessione TCP
		try {
			SocketAddress address= new InetSocketAddress(InetAddress.getByName(ConfigurationSettings.HOST_NAME), ConfigurationSettings.TCP_PORT);
			client= SocketChannel.open(address);
			printWelcome();
			
			String command;
			while (!exit) {
				command= consoleReader.readLine().trim();
				try {
					StringTokenizer tokenizedLine= new StringTokenizer(command);
					String token= tokenizedLine.nextToken();
					
					// in gioco
					if (playing) {
						try {
							SendToServer(token);
							String reply= read();
							String last= reply;
							
							if (reply.contains("#")) last= last.replace("#", "");
							System.out.println(last);
							
							if (reply.contains("Hai tradotto correttamente")) {
								if (!reply.contains("#")) printFromServer(read());
								playing= false;
							}
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					// ricevuta richiesta sfida
					else if (challengeRequest.getToAnswer().get()) {
						HandleChallengeRequest(token);
					}
					// ricevuto comando
					else {
						Parser(token, tokenizedLine);
					}
				}
				catch (NoSuchElementException e) {
					System.out.println(e.getMessage());
				}
			}
		}
		catch (ConnectException e1) {
			e1.printStackTrace();
		}
		catch (IOException e2) {
			e2.printStackTrace();
		}
		try {
			listenerUDP.exit();
			thread.interrupt();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* Stampa lista dei comandi disponibili */
	public static void help (StringTokenizer tokenizedLine) throws IOException {
		try {
			String help_command= "--help";
			SendToServer(help_command);
			String reply= read();
			System.out.println(reply);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println(e2.getMessage());
		}
;	}
	
	/* Inoltra al server comando di login */
	public static void login (StringTokenizer tokenizedLine) throws IOException {
		try {
			String username= tokenizedLine.nextToken();
			String password= tokenizedLine.nextToken();
			if (currUser !=null && currUser.getUserName().equals(username)) throw new AlreadyLoggedIn("Already logged in");
			if (currUser!=null) logout();
			
			String login_command= "login " + username + " " + password + " " + portUDP;
			SendToServer(login_command);
			String reply= read();
			printFromServer(reply);
			if (reply.equals("Login eseguito con successo")) currUser= new User(username,password);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (AlreadyLoggedIn e2) {
			System.out.println(e2.getMessage());
		}
		catch (NoSuchElementException e3) {
			System.out.println("Command error: login <UserName> <password>");
		}
	}
	
	/* Invia comando logout al server */
	public static void logout () throws IOException {
		try {
			if (currUser==null && !exit) {
				System.out.println("Already logged out");
				return;
			}
			String logout_command= "logout " + currUser.getUserName();
			SendToServer(logout_command);
			String reply= read();
			System.out.println(reply);
			if (reply.equals("Logout eseguito con successo")) {
				currUser= null;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println("Command error");
		}
	}
	
	/* Richiede registrazione utente al servizio RMI */
	public static void registra_utente (StringTokenizer tokenizedLine) {
			try {
				ClientRMI clientRMI= new ClientRMI();
				String username= tokenizedLine.nextToken();
				String password= tokenizedLine.nextToken();
				if (clientRMI.registra_utente(username, password)) {
					System.out.println("Registrazione eseguita con successo");
				}
			}
			catch (NoSuchElementException e) {
				System.out.println("Command error: registra_utente <UserName> <password>");
			}
	}
	
	/* Invia comando aggiungi_amico al server */
	public static void aggiungi_amico (StringTokenizer tokenizedLine) throws IOException {
		try {
			String friend= tokenizedLine.nextToken();
			if (currUser==null) {
				System.out.println("You must be logged in!");
				return;
			}
			String aggiungiAmico_command= "aggiungi_amico " + currUser.getUserName() + " " + friend;
			SendToServer(aggiungiAmico_command);
			String reply= read();
			System.out.println(reply);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println("Command error: aggiungi_amico <UserName>");
		}
	}
	
	/* Invia comando lista_amici al server */
	public static void lista_amici () throws IOException {
		try {
			if (currUser==null) {
				System.out.println("You must be logged in!");
				return;
			}
			
			String listaAmici_command= "lista_amici " + currUser.getUserName() + "";
			SendToServer(listaAmici_command);
			String reply= read();
			JSONUtils.printFriendsJSON(reply);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println("Command error: lista_amici");
		}
	}
	/* EFFECTS: Invia comando lista_amici al server e una volta ricevuto il JSON lo stampa */
	
	/* Invia comando sfida al server */
	public static void sfida (StringTokenizer tokenizedLine) throws IOException {
		try {
			if (currUser==null) {
				System.out.println("You must be logged in!");
				return;
			}
			String challenged= tokenizedLine.nextToken();
			printFromServer("In attesa di una risposta per la sfida...");
			String challenge_command= "sfida " + currUser.getUserName() + " " + challenged + "";
			SendToServer(challenge_command);
			String reply= read();
			System.out.println(reply);
			if (reply.contains("accepted the challenge")) {
				playing= true;
			}
			
			try {
				if (playing) {
					System.out.println("Attendi generazione parole...");
					printFromServer(read());
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		catch (NoSuchElementException e) {
			System.out.println("Command error: sfida <UserName>");
		}
	}
	
	/* Invia comando mostra_punteggio al server */
	public static void mostra_punteggio () throws IOException {
		try {
			if (currUser==null) {
				System.out.println("You must be logged in!");
				return;
			}
			String mostraPunteggio_command= "mostra_punteggio " + currUser.getUserName() + "";
			SendToServer(mostraPunteggio_command);
			String reply=  read();
			System.out.println(reply);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println("Command error: mostra_punteggio");
		}
	}
	
	/* Invia comando mostra_classifica al server */
	public static void mostra_classifica () {
		try {
			if (currUser==null) {
				System.out.println("You must be logged in!");
				return;
			}
			String mostraClassifica_command= "mostra_classifica " + currUser.getUserName() + "";
			SendToServer(mostraClassifica_command);
			String reply= read();
			// trasformo JSON object in HashMap
			final ObjectMapper mapper= new ObjectMapper();
			ArrayList<String> ranking= mapper.reader()
					.withType(new TypeReference<ArrayList<String>> () {})
					.readValue(reply.getBytes());
			// stampo
			JSONUtils.printRankingJSON(ranking);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (NoSuchElementException e2) {
			System.out.println("Command error: mostra_classifica");
		}
	}
	/* EFFECTS: Invia il comando di mostra_classifica e una volta ricevuto il JSON
	 * lo trasforma in HashMap e poi lo stampa
	 */
	
	/* Gestisce la ricezione di una richiesta di sfida */
	private static void HandleChallengeRequest (String token) throws IOException {
		if (TimestampUtils.isTimeExpired(challengeRequest.getTimeout())) {
			listenerUDP.setChallengeReply("notAnswered");
			playing= false;
			System.out.println("Timeout: challenge request expired");
			return;
		}
		
		if (token.equals("si")) {
			listenerUDP.setChallengeReply("si");
			System.out.println("Challenge accepted");
			playing= true;
			printFromServer(read());
		}
		else if (token.equals("no")) {
			listenerUDP.setChallengeReply("no");
			System.out.println("Challenge declined");
			playing= false;
		}
		else {
			System.out.println("Invalid answer: [si/no]");
		}
	}
	/* EFFECTS: Richiede una risposta, relativa ad una richiesta di sfida, al client 
	 * oppure stampa messaggio di timeout se non la riceve
	 */
	
	/* Invia messaggio al server */
	public static void SendToServer (String message) {
		try {
			message+= "\n";
			client.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* Leggi input client */
	private static String read () throws IOException {
		ByteBuffer buff= ByteBuffer.allocate(1024);
		client.read(buff);
		String reply= new String(buff.array()).trim();
		buff.clear();
		return reply;
	}
	
	/* Stampa al client la risposta del server */
	private static void printFromServer (String message) {
		if (message.contains("#")) message= message.replace("#", "");
		System.out.println(message);
	}
	
	/* Metodo ausiliario per stampare messaggio benvenuto */
	private static void printWelcome () {
		System.out.println("Welcome to WordQuizzle, you can print the commands |--help| for a list of commands");
	}
	
	/* Metodo ausiliario per stampare lista comandi */
	private static void PrintHelpMessage () {
		System.out.println("usage : COMMAND [ ARGS ...]\n"
				+ "Commands: \n"
				+ "exit per uscire dal servizio\n"
				+ "registra_utente <nickUtente> <password> registra l'utente\n"
				+ "login <nickUtente> <password> effettua il login\n"
				+ "logout effettua il logout\n"
				+ "aggiungi_amico <nickAmico> crea relazione amicizia con nickAmico\n"
				+ "lista_amici mostra la lista dei propri amici\n"
				+ "sfida <nickAmico> richiesta di una sfida a nickAmico\n"
				+ "mostra_punteggio mostra il punteggio dell'utente\n"
				+ "mostra_classifica mostra una classifica degli amici dell'utente (incluso l'utente stesso");
	}
	
	/* Parser dei comandi */
	private static void Parser (String token, StringTokenizer tokenizedLine) {
		try {
			if (token.equals("--help")) {
				PrintHelpMessage();
				return;
			}
			if (token.equals("exit")) {
				exit= true;
				logout();
				client.close();
				return;
			}
			if (token.equals("login")) {
				login(tokenizedLine);
				return;
			}
			if (token.equals("logout")) {
				logout();
				return;
			}
			if (token.equals("registra_utente")) {
				registra_utente(tokenizedLine);
				return;
			}
			if (token.equals("aggiungi_amico")) {
				aggiungi_amico(tokenizedLine);
				return;
			}
			if (token.equals("lista_amici")) {
				lista_amici();
				return;
			}
			if (token.equals("sfida")) {
				sfida(tokenizedLine);
				return;
			}
			if (token.equals("mostra_punteggio")) {
				mostra_punteggio();
				return;
			}
			if (token.equals("mostra_classifica")) {
				mostra_classifica();
				return;
			}
			System.out.println("Command not found, you can type --help for list of commands");
			return;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}	
}
