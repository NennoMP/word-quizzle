package Client;

import java.net.DatagramPacket;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;

import Server.ConfigurationSettings;
import Server.TimestampUtils;

/** Classe runnable che funge da listener per le richieste di sfida su UDP */

public class ListenerUDP implements Runnable {
	private int portUDP;
	private DatagramSocket socketServer;
	
	private ChallengeRequest challengeRequest;
	private AtomicBoolean challengeAnswered;
	private String challengeReply;
	
	// costruttore
	public ListenerUDP (int portUDP) {
		this.challengeRequest= ChallengeRequest.getInstance();
		this.portUDP= portUDP;
		this.challengeReply= "";
		this.challengeAnswered= new AtomicBoolean(false);
	}
	
	
	/* Apre server UDP per restare in ascolto di richieste sfida */
	public void run () {
		byte[] buff= new byte[100];
		DatagramPacket packet= new DatagramPacket(buff, buff.length);
		
		// apro server UDP
		try {
			socketServer= new DatagramSocket(portUDP, InetAddress.getByName(ConfigurationSettings.HOST_NAME));
			while (!Thread.currentThread().isInterrupted()) {
				socketServer.receive(packet);
				String challengerName= new String(packet.getData());
				System.out.println(challengerName.trim() + " desidera sfidarti, accetti? [si/no]");
				Timestamp endTime= TimestampUtils.setEndTimestamp(new Timestamp(System.currentTimeMillis()), ConfigurationSettings.TIME_CHALLENGE_REQUEST);
				challengeRequest.setTimeout(endTime);
				challengeRequest.setToAnswer(new AtomicBoolean(true));
				
				// aspetto risposta
				while (!Thread.currentThread().isInterrupted() && !this.challengeAnswered.get());
				if (Thread.currentThread().isInterrupted()) return;
				
				String reply= this.challengeReply;
				if (this.challengeAnswered.get() && (reply.equals("si") || reply.equals("no"))) {
					reply= this.challengeReply;
					this.challengeAnswered.set(false);
					byte[] buffACK= reply.getBytes(StandardCharsets.UTF_8);
					DatagramPacket packetACK= new DatagramPacket(buffACK, buffACK.length, InetAddress.getByName(ConfigurationSettings.HOST_NAME), packet.getPort());
					socketServer.send(packetACK);
				}
				else {
					this.challengeReply= "";
				}
				challengeRequest.setToAnswer(new AtomicBoolean(false));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/* EFFECTS: Gestisce la richiesta di sfida e memorizza, e inoltra verso User sfidante, la risposta
	 * dell'utente o il timeout se non risponde
	 */
	
	/* Imposta la risposta alla richiesta di sfida */
	public void setChallengeReply (String challengeReply) {
		this.challengeAnswered.set(true);
		this.challengeReply= challengeReply;
	}
	
	/* Comando Exit del client mi fa uscire dal listening su porta UDP */
	public void exit () {
		if (socketServer!=null) socketServer.close();
	}
}
	
