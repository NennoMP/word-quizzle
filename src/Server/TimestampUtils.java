package Server;

import java.sql.Timestamp;

/** Classe contenente alcuni metodi utili per la gestione dei Timestamp (utilizzati per organizzare i timeout di richiesta sfida e partita) **/

public class TimestampUtils {
	
	/* Restituisce true/false per la scadenza del timeout */
	public static boolean isTimeExpired (Timestamp timeout) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return timeout.before(now);
    }
	
	/* Aggiunge secondi ad un timestamp per impostare l'endtime */
	public static Timestamp setEndTimestamp (Timestamp startTime, int seconds) {
        return new Timestamp(startTime.getTime() + (seconds * 1000L));
    }

}
