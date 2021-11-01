package Server;


import java.util.Iterator;

/** Classe runnable che si occupa della gestione della sfida, quindi invio dei messaggi di conclusione (vittoria/sconfitta) 
 * agli utenti e terminazione di una sfida, che sia per timeout o perche' entrambi gli utenti concludono in tempo.
 * In caso di timeout invierà le statistiche all'utente quando proverà a scrivere un messaggio dopo il termine (che non verrà registrato)
 */

public class ChallengeRunnable implements Runnable {
	ChallengesList challengesList;
	
	// costruttore
	public ChallengeRunnable () {
		challengesList= ChallengesList.getInstance();
	}
	
	/* Effettua il pooling sulla lista delle sfide in esecuzione e invia messaggi postgame */
	public void run () {
		while (!Thread.currentThread().isInterrupted()) {
			Iterator<Challenge> iterator= challengesList.getHashListaSfide().values().iterator();
			while (iterator.hasNext()) {
				Challenge challenge= iterator.next();
				if (challenge==null) continue;
				Match mChallenger= challenge.getMatchChallenger();
				Match mChallenged= challenge.getMatchChallenged();
				if (mChallenger==null || mChallenged==null) continue;
				if (!(TimestampUtils.isTimeExpired(mChallenger.getEnd()) && TimestampUtils.isTimeExpired(mChallenged.getEnd())) && !(mChallenger.isEnded() && mChallenged.isEnded())) continue;
				trySendStats(mChallenger);
				trySendStats(mChallenged);
				
				// Challenger wins
				if (mChallenger.getScore() > mChallenged.getScore()) {
					mChallenger.getUser().UpdateScore(ConfigurationSettings.WIN_POINTS);
					mChallenger.sendTextToClient(String.format("Il tuo avversario ha totalizzato %d punti.\nCongratulazioni, hai vinto! Hai guadagnato %d punti extra, per un totale di %d punti!#", mChallenged.getScore(), ConfigurationSettings.WIN_POINTS, mChallenger.getScore()));
					mChallenged.sendTextToClient(String.format("Il tuo avversario ha totalizzato %d punti.\nPeccato, hai perso! Non hai guadagnato punti extra questa volta#", mChallenger.getScore()));
				}
				// Challenged wins
				else if (mChallenger.getScore() < mChallenged.getScore()) {
					mChallenged.getUser().UpdateScore(ConfigurationSettings.WIN_POINTS);
					mChallenged.sendTextToClient(String.format("Il tuo avversario ha totalizzato %d punti.\nCongratulazioni, hai vinto! Hai guadagnato %d punti extra, per un totale di %d punti!#", mChallenger.getScore(), ConfigurationSettings.WIN_POINTS, mChallenged.getScore()));
					mChallenger.sendTextToClient(String.format("Il tuo avversario ha totalizzato %d punti.\nPeccato, hai perso! Non hai guadagnato punti extra questa volta#", mChallenged.getScore()));
				}
				// Pareggio
				else {
					String tie= String.format("Pareggio! Non guadagni punti extra questo round#");
					mChallenger.sendTextToClient(tie);
					mChallenged.sendTextToClient(tie);
				}
				// salvataggio su JSON file dei punteggi
				JSONUtils.SaveJsonFile();
				
				mChallenger.getUser().setPlaying(false);
				mChallenged.getUser().setPlaying(false);
				challengesList.removeChallenge(challenge);
				System.out.println("This challenge has ended");
				iterator.remove();
			}
		}
	}
	
	/* Invia statistiche se match concluso pre-timeout */
	private void trySendStats (Match match) {
        if (match.isEnded()) return;
        match.setEnded(true);
        match.SetStatsNow();
        match.SendResultToClient();
    }
	
	
}
