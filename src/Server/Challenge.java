package Server;

import java.io.BufferedReader;



import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.JsonObject;

/** Classe che rappresenta la sfida tra due utenti, contiene i rispettivi Match, i quali sono personali con i relativi dati, e <parole,traduzioni>
 * associate alla sfida e di conseguenza alle partite
 */

public class Challenge {
	private int challengeID;
	
	private Match mChallenger, mChallenged;
	private ArrayList<String> words;
	private ArrayList<String> translations;
	
	// costruttore
	public Challenge (int challengeID) {
		int n= ConfigurationSettings.MATCH_WORDS;
		this.challengeID= challengeID;
		this.words= new ArrayList<String>();
		this.translations= new ArrayList<String>();
		this.words= Dictionary.getInstance().getWords();
		try {
			for (int i=0;i<n;i++) {
				String s= getTranslation(words.get(i));
				this.translations.add(i, s);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	/* EFFECTS: Carica n_default_words (5) parole e rispettive traduzioni */
	
	/* Restituisce identifier sfida */
	public int getID () {
		return challengeID;
	}
	
	/* Restituisce la lista di parole da tradurre */
	public ArrayList<String> getWords () {
		return words;
	}
	
	/* Restituisce la lista delle traduzioni */
	public ArrayList<String> getTranslations () {
		return translations;
	}
	
	/* Richiede la traduzione di una parola all' API */
	private static String getTranslation (String word) throws IOException {
		URL url= new URL(ConfigurationSettings.API_URL + word);
		String translation;
		try (JsonReader reader= new JsonReader(new BufferedReader(new InputStreamReader(url.openStream())))) {
			JsonParser parser= new JsonParser();
			JsonObject jsonObject= parser.parse(reader).getAsJsonObject();
			translation= jsonObject.get("responseData").getAsJsonObject().get("translatedText").getAsString();
		}
		System.out.printf("[WORDS] %s translated into %s\n", word, translation);
		return translation;
	}
	
	/* Restituisce oggetto Match dello sfidante */
	public Match getMatchChallenger () {
		return mChallenger;
	}
	
	/* Restituisce oggetto Match dello sfidato */
	public Match getMatchChallenged () {
		return mChallenged;
	}
	
	/* Imposta i Match di sfidante e sfidato */
	public void setMatches (Match mChallenger, Match mChallenged) {
		if (this.mChallenger==null) this.mChallenger= mChallenger;
		if (this.mChallenged==null) this.mChallenged= mChallenged;
	}
}
