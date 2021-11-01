package Server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Exceptions.AlreadyLoggedIn;
import Exceptions.ChallengeDeclined;
import Exceptions.FriendAlreadyExists;
import Exceptions.FriendAlreadyPlaying;
import Exceptions.FriendNotFound;
import Exceptions.NoChallengeAnswer;
import Exceptions.UserNotFound;
import Exceptions.UserNotLoggedIn;
import Exceptions.UserNotOnline;
import Exceptions.WrongPassword;

/**  Classe che rappresenta il server TCP che gestisce l'interazione con i client tramite l'utilizzo di un threadpool ed un selector (NIO),
 * effettua il parsin dei comandi ricevuti ed esegue i rispettivi metodi apportando modifiche al file JSON quando necessario (aggiungi_amico e sfida).
 * E' presente un thread, che esegue la classe Runnable: ChallengeRunnable, thread_challenges che effettua pooling sulle sfide in esecuzione e un threadpool che esegue e gestisce
 * le partite dei vari utenti.
 * Per la parte di Accettazione, Scrittura e Lettura in socket durante l'interazione con il client viene utilizzato un attachment che mantiene un ByteBuffer (messaggio)
 * e un oggetto User ottenuto con il costruttore (REGISTRATION) di User che lo inizializza con la SelectionKey
 * **/

public class Server {
	// utente corrente
	private User currUser;
	
	// channels
	SocketChannel channelClient;
	ServerSocketChannel channelServer;
	
	// threadpool & selector
	ExecutorService executor;
	Selector selector;
	
	/* Start di trheadpool (gestione clienti), thread gestorione sfide e server TCP */
	public void start () {
		System.out.println("WordQuizzle-Sever has started.");
		try {
			// apro channels
			channelServer= ServerSocketChannel.open();
			channelServer.socket().bind(new InetSocketAddress(InetAddress.getByName(ConfigurationSettings.HOST_NAME), ConfigurationSettings.TCP_PORT));
			channelServer.configureBlocking(false);
			
			// inizializzo threadpool & selector
			executor= Executors.newFixedThreadPool(ConfigurationSettings.N_THREADS);
			int ops= channelServer.validOps();
			selector= Selector.open();
			channelServer.register(selector, ops, null);
			
			// start thread sfide
			ChallengeRunnable challengeRunnable= new ChallengeRunnable();
			Thread thread_challenges= new Thread(challengeRunnable);
			thread_challenges.start();
			
			while(true) {
				selector.select();
				Set<SelectionKey> selKeys= selector.selectedKeys();
				Iterator<SelectionKey> iterator= selKeys.iterator();
				
				while (iterator.hasNext()) {
					SelectionKey key= iterator.next();
					iterator.remove();
					
					try {
						// Nuovo client
						if (key.isAcceptable()) {
							SocketChannel client = channelServer.accept(); 
					        client.configureBlocking(false); 
					        SelectionKey SelKeys = client.register(selector, SelectionKey.OP_READ);
					        ByteBuffer message = ByteBuffer.allocate(1024);
					        ByteBuffer[] buff = {message};
					        currUser = new User(SelKeys);
					        // creo attachment
					        Object[] objClient = {buff, currUser};
					        SelKeys.attach(objClient); 
						}
						// Il Client ha scritto sulla socket
						else if (key.isReadable()) {
							channelClient = (SocketChannel) key.channel();
							// recupero attachment
					        Object[] objClient = (Object[]) key.attachment();
					        ByteBuffer[] bfs = (ByteBuffer[]) objClient[0];
					        
					        long byteLeft = channelClient.read(bfs); 
					        if (byteLeft == -1) throw new IOException();
					        ByteBuffer msgBuf = bfs[0]; 
					        StringBuilder message = new StringBuilder();
					        msgBuf.flip();
					        byte[] bytes = new byte[msgBuf.remaining()];
					        msgBuf.get(bytes);
					        message.append(new String(bytes)); 
					        msgBuf.clear();
					        byteLeft = channelClient.read(msgBuf);  
					        if (byteLeft == -1) throw new IOException();
					        this.currUser = (User) objClient[1]; 
					        // parsing comandi
					        if (!message.toString().isEmpty()) Parser(message.toString());
							
						}
						// Rispondi al client
						else if (key.isWritable()) {
							channelClient = (SocketChannel) key.channel(); 
							// recupero attachment
					        Object[] respObj = (Object[]) key.attachment(); 
					        String response = (String) respObj[0];
					        
					        ByteBuffer respBuf = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
					        channelClient.write(respBuf);
					        if (!respBuf.hasRemaining()) {
					            respBuf.clear();
					            ByteBuffer msg = ByteBuffer.allocate(1024);
					            ByteBuffer[] bfs = {msg};
					            Object[] objClient = {bfs, this.currUser};
					            channelClient.register(selector, SelectionKey.OP_READ, objClient);
					        }
							
						}
					}
					catch (IOException e) {
						System.out.println("Connection timeout");
						key.channel().close();
						key.cancel();
						e.printStackTrace();
					}
				}
			}
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	/* Effettua il login di currUser */
	public void login (String UserName, String password) throws IOException, IllegalArgumentException, AlreadyLoggedIn, WrongPassword {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal Username");
			if (password==null || password.equals("")) throw new IllegalArgumentException("Illegal password");
			if (iru.isLogged(UserName)) throw new AlreadyLoggedIn("User already logged in");
			User u= iru.getUser(UserName);
			if (!u.getPassword().equals(password)) throw new WrongPassword("Wrong password");
			
			iru.setLogged(UserName, true);
			u.setKey(this.currUser.getKey());
			u.setUDP(this.currUser.getUDP());
			this.currUser= u;
			System.out.println(this.currUser + ": " + UserName + " logged");
			replyToClient("Login eseguito con successo");
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	/* EFFECTS: Connette l'utente se non lo è già e risponde al client con 
	 * messaggio di conferma o di errore 
	 */
	
	/* Effettua il logout di currUser */
	public void logout (String UserName) throws ClosedChannelException, IllegalArgumentException, UserNotLoggedIn, UserNotFound {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException();
			if (!iru.isLogged(UserName)) throw new UserNotLoggedIn ("User not logged in");
			
			iru.setLogged(UserName, false);
			replyToClient("Logout eseguito con successo");
			System.out.println(this.currUser + ": " + UserName + " logged out");
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	/* EFFECTS: Disconnette l'utente se non lo è già e risponde al client con
	 * messaggio di conferma o di errore
	 */
	
	/* Aggiunge un nuovo amico in lista amici di currUser */
	public void aggiungi_amico (String UserName, String FriendName) throws ClosedChannelException, IllegalArgumentException, UserNotFound, FriendAlreadyExists {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal Username");
			if (FriendName==null || FriendName.equals("")) throw new IllegalArgumentException("Illegal friend name");
			if (UserName.equals(FriendName)) throw new IllegalArgumentException("You can't add yourself as a friend");
			User u= iru.getUser(UserName);
			User friend= iru.getUser(FriendName);
			if (u==null || friend==null) throw new UserNotFound("User not found");
			if (u.getFriends().containsKey(FriendName)) throw new FriendAlreadyExists("Already friends");
			if (!friend.isLogged()) throw new UserNotOnline("Friend is not online");
			
			iru.getUser(UserName).addFriend(FriendName);
			iru.getUser(FriendName).addFriend(UserName);
			JSONUtils.SaveJsonFile();
			replyToClient("Amicizia " + UserName + "-" + FriendName + " creata");
			System.out.println(UserName + " added " + FriendName);
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	/* EFFECTS: Inserisce l'amicizia, se non già presente, nella lista amici di currUser e anche
	 * dell'amico aggiunto, risponde al client con messaggio di conferma o di errore
	 */
	
	/* Restituisce la lista amici di currUser */
	public void lista_amici (String UserName) throws ClosedChannelException, IllegalArgumentException, UserNotFound {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		try {
			System.out.println(UserName);
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal username");
			User u= iru.getUser(UserName);
			if (u==null) throw new UserNotFound ("User not found");
			
			ConcurrentHashMap<String,String> friendsList= iru.getUser(UserName).getFriends();
			String friendsJSON= JSONUtils.createJSON(friendsList);
			replyToClient(friendsJSON);
			System.out.println(this.currUser + ": " + this.currUser.getUserName() + " asked friends list of " + UserName);
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	/* EFFECTS: Crea un JSON contente la lista amici e lo invia al client */
	
	/* Richiede e gestisce una sfida */
	public void sfida (String UserName, String FriendName) throws IOException {
		
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal Username");
			if (FriendName==null || FriendName.equals("")) throw new IllegalArgumentException("Illegal friend name");
			if (UserName.equals(FriendName)) throw new IllegalArgumentException("You can't challenge yourself");
			User challenger= RegisteredUsers.getInstance().getUser(UserName);
			if (!challenger.isFriend(FriendName)) throw new FriendNotFound("You're not friend with the specified challenged user");
			User challenged= RegisteredUsers.getInstance().getUser(FriendName);
			if (challenged!=null && !challenged.isLogged()) throw new UserNotOnline("The specified friend is not online");
			if (challenged.isPlaying()) throw new FriendAlreadyPlaying("The specified friend is already in a game");
			
			challenger.setPlaying(true);
			challenged.setPlaying(true);
			sendChallengeUDP(UserName, challenger, challenged);
			channelClient.write(ByteBuffer.wrap((FriendName + " accepted the challenge").getBytes(StandardCharsets.UTF_8)));
			Challenge challengeObj= new Challenge(challenger.getUserName().hashCode() + new Random().nextInt(4));
			Match mChallenger= new Match(challenger, challengeObj);
			Match mChallenged= new Match(challenged, challengeObj);
			challengeObj.setMatches(mChallenger, mChallenged);
			ChallengesList.getInstance().addChallenge(challengeObj);
			executor.execute(mChallenger);
			executor.execute(mChallenged);
			System.out.println(UserName + " challenged " + FriendName);
		}
 		catch (Exception e2) {
			User challenged= RegisteredUsers.getInstance().getUser(FriendName);
			if (currUser.isPlaying()) currUser.setPlaying(false);
			if (challenged!=null && challenged.isPlaying()) challenged.setPlaying(false);
			replyToClient(e2.getMessage());
		}
	}
	/* EFFECTS: Inoltra una richiesta di sfida (sendChallengeUDP) all'utente sfidato e attende una risposta,
	 * notifica quest'ultima o un timeout all'utente sfidante, prosegue poi con la sfida e relativo
	 * aggiornamento dei punteggi
	 */
	
	/* Restituisce il punteggio di currUser */
	public void mostra_punteggio (String UserName) throws ClosedChannelException, IllegalArgumentException, UserNotFound {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException();
			User u= iru.getUser(UserName);
			if (u==null) throw new UserNotFound("User not found");
			
			System.out.println(u.getTotalScore());
			long score= iru.getUser(UserName).getTotalScore();
			replyToClient("Punteggio: " + score);
			System.out.println(this.currUser + ": " + this.currUser.getUserName() + " asked for " + UserName + "'s score");
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	
	/* Restituisce la classifica di currUser e amici */
	public void mostra_classifica (String UserName) throws ClosedChannelException, IllegalArgumentException, UserNotFound {
		try {
			if (UserName==null || UserName.equals("")) throw new IllegalArgumentException("Illegal username");
			User u= RegisteredUsers.getInstance().getUser(UserName);
			if (u==null) throw new UserNotFound("User not found");
			
			ArrayList<User> rankingTmp = new ArrayList<>();
	        rankingTmp.add(u);
	        if (!u.getFriends().isEmpty()) { 
	            for (String f : u.getFriends().values()) {
	                User friend = RegisteredUsers.getInstance().getUser(f);
	                rankingTmp.add(friend);
	            }
	            // sorto in base al punteggio totale
	            rankingTmp.sort(Comparator.comparing(User::getTotalScore).reversed());
	        }
	        // elimino informazioni non necessarie (mantengo solo <username,punteggio
	        ArrayList<String> result = new ArrayList<>(); 
	        for (User user : rankingTmp) result.add(user.getUserName() + " " + user.getTotalScore());

	        String rankingJSON= JSONUtils.createJSON(result);
	        replyToClient(rankingJSON);
	        System.out.println(this.currUser + ": " + this.currUser.getUserName() + " asked for ranking of user " + UserName);
		}
		catch (Exception e) {
			replyToClient(e.getMessage());
		}
	}
	/* EFFECTS: Crea un JSON contenente la classifica e la restituisce al client */
	
	/* Effettua il parsing dei comandi (utilizzato in key.isReadable) */
	private void Parser (String message) {
		if (currUser.isPlaying()) return;
		StringTokenizer tokenizedLine= new StringTokenizer(message);
		String command= tokenizedLine.nextToken();
		String UserName, pwd, portUDP, FriendName;
		try {
			if (command.equals("login")) {
				UserName= tokenizedLine.nextToken();
				pwd= tokenizedLine.nextToken();
				portUDP= tokenizedLine.nextToken();
				this.currUser.setUDP(Integer.parseInt(portUDP));
				login(UserName,pwd);
				return;
			}
			if (command.equals("logout")) {
				UserName= tokenizedLine.nextToken();
				logout(UserName);
				return;
			}
			if (command.equals("aggiungi_amico")) {
				UserName= tokenizedLine.nextToken();
				FriendName= tokenizedLine.nextToken();
				aggiungi_amico(UserName,FriendName);
				return;
			}
			if (command.equals("lista_amici")) {
				UserName= tokenizedLine.nextToken();
				lista_amici(UserName);
				return;
			}
			if (command.equals("sfida")) {
				UserName= tokenizedLine.nextToken();
				FriendName= tokenizedLine.nextToken();
				sfida(UserName,FriendName);
				return;
			}
			if (command.equals("mostra_punteggio")) {
				UserName= tokenizedLine.nextToken();
				mostra_punteggio(UserName);
				return;
			}
			if (command.equals("mostra_classifica")) {
				UserName= tokenizedLine.nextToken();
				mostra_classifica(UserName);
				return;
			}
		}
		catch (IOException | NoSuchElementException e) {
			e.printStackTrace();
		}
	}
	
	/* Risponde al client */
	private void replyToClient (String message) throws IllegalArgumentException, NullPointerException, ClosedChannelException {
		if (message==null || message.equals("")) throw new IllegalArgumentException();
		if (channelClient==null) throw new NullPointerException();
		Object[] objReply= {message + "\n", this.currUser};
		channelClient.register(selector, SelectionKey.OP_WRITE, objReply);
	}
	
	/* Metodo ausiliario che invia richiesta di sfida (utilizzato in SFIDA) */
	private void sendChallengeUDP (String UserName, User challenger, User challenged) throws IOException, NoChallengeAnswer, ChallengeDeclined {
		DatagramSocket clientUDP = new DatagramSocket();
		// setto timeout richiesta
        clientUDP.setSoTimeout(ConfigurationSettings.UDP_TIMEOUT);
        
        InetAddress address = InetAddress.getByName(ConfigurationSettings.HOST_NAME);
        String message = UserName; 
        byte[] buff = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(buff, buff.length, address, challenged.getUDP());
        clientUDP.send(packet);
        
        // ACK per risposta
        byte[] ack = new byte[2];
        DatagramPacket packetACK = new DatagramPacket(ack, ack.length);

        try {
        	// se ho una risposta la invio
            clientUDP.receive(packetACK); 
            message = new String(packetACK.getData());
        } catch (SocketTimeoutException e) {
        	// se ho timeout notifico e resetto stato playing
            System.out.println(e.getMessage() + ": challenge between " + " " + challenged.getUserName());
            clientUDP.close();
            challenger.setPlaying(false);
            challenged.setPlaying(false);
            throw new NoChallengeAnswer("No answer for the challenge you sent");
        }
        clientUDP.close(); 
        
        if (message.equals("no")) { 
        	System.out.println(challenged.getUserName() + " declinded challenge from " + UserName);
            challenger.setPlaying(false);
            challenged.setPlaying(false);
            throw new ChallengeDeclined("The challenge got declined");
        }
	}
}
