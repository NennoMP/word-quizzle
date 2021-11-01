package Server;

import java.io.IOError;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Classe runnable che rappresenta la partita di un singolo utente con relative statistiche
 *  e risposte date, insieme a parole e traduzioni. In caso di timeout invia le statistiche all'utente quando proverà
 *  a scrivere un messaggio dopo il timeout (che non sarà registrato)
 * Thread che gestisce le interazioni utente */

public class Match implements Runnable {
	private User user;
	SocketChannel client;
	
	// match-info
	private ArrayList<String> words;
	private ArrayList<String> translations;
	private int n_words;
	private int n_correct;
	private int n_missed;
	private int n_unanswered;
	private int score;
	
	// Timestamp for timeout
	public Timestamp startTime;
	public Timestamp endTime;
	public AtomicBoolean ended;
	
	// costruttore
	public Match (User user, Challenge challenge) {
		this.n_words= ConfigurationSettings.MATCH_WORDS;
		this.user= user;
		this.client= (SocketChannel) user.getKey().channel();
		this.words= challenge.getWords();
		this.translations= challenge.getTranslations();
		this.n_correct= 0;
		this.n_missed= 0;
		this.n_unanswered= 0;
		this.score= 0;
		this.startTime= new Timestamp(System.currentTimeMillis());
		this.endTime= TimestampUtils.setEndTimestamp(this.startTime, ConfigurationSettings.TIME_GAME);
		this.ended= new AtomicBoolean(false);
	}
	/* EFFECTS: Carica n_words_default (5) random parole e rispettive traduzioni, inizializza startTime e setta endTime
	 * per il timeout e le statistiche
	 */
	
	/* Fa iniziare la partita */
	public void run () {
		try {
			int i= 0;
			String pregame= "Via alla sfida di traduzione!\nAvete 60 secondi per tradurre correttamente " + ConfigurationSettings.MATCH_WORDS + " parole.";
			do {
				String word= words.get(i);
				String translation= translations.get(i);
				String ingame= String.format("Challenge %d/%d: %s", i+1, this.n_words, word);
				
				if (i==0) {
					sendTextToClient(pregame + "\n" + ingame);
				}
				else {
					sendTextToClient(ingame);
				}
				
				String answer= readAnswer();
				
				if (!TimestampUtils.isTimeExpired(this.endTime)) {
					if (answer.equals(translation.toLowerCase())) {
						this.n_correct++;
					}
					else {
						this.n_missed++;
					}
				}
			} while (!TimestampUtils.isTimeExpired(this.endTime) && (++i < this.n_words));
			
			if (!ended.get()) {
				this.n_unanswered= this.n_words - (this.n_correct + this.n_missed);
				this.score= (this.n_correct * ConfigurationSettings.CORRECT_ANSWER_POINTS) + (this.n_missed * ConfigurationSettings.WRONG_ANSWER_POINTS);
				user.UpdateScore(this.score);
				SendResultToClient(); 
			}
			ended.set(true);
		}
		catch (Exception e) {
			ended.set(true);
			user.setPlaying(false);
			e.printStackTrace();
		}
	}
	/* EFFECTS: Invia i messaggi di pregame, ingame e postgame, calcola ed invia le statistiche personali dell'utente,
	 * interrompe la sfida se non conclusa prima del timeout ed invia comunque le statistiche */
	
	/* Restituisce oggetto User */
	public User getUser () {
		return user;
	}
	
	/* Restituisce punteggio */
	public int getScore () {
		return this.score;
	}
	
	/* Restituisce Timestamp di timeout */
	public Timestamp getEnd() {
		return endTime;
	}
	
	/* Restituisce boolean stato ended di partita */
	public boolean isEnded () {
		return ended.get();
	}
	
	/* Imposta boolean stato ended di partita a 'bool' */
	public void setEnded (boolean bool) {
		this.ended.set(bool);
	}
	
	/* Legge risposta dell'utente */
	public String readAnswer () throws IOException, NullPointerException {
		if (client==null) throw new NullPointerException();
		
		ByteBuffer message= ByteBuffer.allocate(1024);
		long byteRemaining;
		do {
			byteRemaining= client.read(message);
			if (byteRemaining==-1) throw new IOException();
		} while (byteRemaining==0);
		return new String(message.array()).toLowerCase().trim();
	}
	
	/* Invia testo pregame e ingame all'utente */
	public void sendTextToClient (String s) throws IllegalArgumentException , NullPointerException {
		if (s==null || s.equals("")) throw new IllegalArgumentException();
		if (client==null) throw new NullPointerException();
		
		try {
			client.write(ByteBuffer.wrap((s + "\n").getBytes(StandardCharsets.UTF_8)));
		}
		catch (IOError | IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Calcola le statistiche a partita concluse */
	public void SetStatsNow () {
		this.n_unanswered= this.n_words - (this.n_correct + this.n_missed);
		this.score= (this.n_correct * ConfigurationSettings.CORRECT_ANSWER_POINTS) + (this.n_missed * ConfigurationSettings.WRONG_ANSWER_POINTS);
		user.UpdateScore(this.score);
	}
	
	/* Invia statistiche (testo postgame) all'utente */
	public void SendResultToClient () throws NullPointerException {
		if (client==null) throw new NullPointerException();
		String result= String.format("Hai tradotto correttamente %d parole, ne hai sbagliate %d e non risposto a %d.\nHai totalizzato %d punti.", this.n_correct, this.n_missed, this.n_unanswered, this.score);
		
		try {
			client.write(ByteBuffer.wrap((result + "\n").getBytes(StandardCharsets.UTF_8)));
		}
		catch (IOError | IOException e) {
			e.printStackTrace();
		}
	}
}
