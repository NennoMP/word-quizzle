package Server;

import java.io.BufferedReader;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

/** Classe contenente metodi utili per la gestione delle interazioni con il dizionario **/

public class Dictionary {
	private ArrayList<String> dictionary;
	public static Dictionary instance;
	
	// costruttore
	private Dictionary () {
		this.dictionary= new ArrayList<>();
		loadDictionary(ConfigurationSettings.FILE_WORDS);
	}
	/* EFFECTS: Carica tutte le parole del dizionario nell'ArrayList words */
	
	// (DOUBLE-CHECKED LOCKING) Implementazione Singleton ottimizzata, uso synchronized solo se ==null
	public static Dictionary getInstance() {
		if (instace==null) 
			synchronized (Dictionary.class) {
				if (instance==null) instance= new Dictionary();
			}
		return instance;
	}
	
	/* Restituisce l'arraylist delle parole */
	public ArrayList<String> getDictionary() {
		return dictionary;
	}
	
	/* Carica tutte le parole dal dizionario */
	public void loadDictionary (String path) {
		try (BufferedReader reader= Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
			String curr;
			while ((curr=reader.readLine())!=null) {
				this.dictionary.add(curr);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Prende n_word_default (5) parole random per la partita */
	public synchronized ArrayList<String> getWords () {
		int n= ConfigurationSettings.MATCH_WORDS;
		if (n> this.dictionary.size()) throw new IllegalArgumentException("Not enough words in the dictionary: size is " + this.dictionary.size());
		
		// Garantisce parole diverse per ogni partita
		Collections.shuffle(this.dictionary);
		ArrayList<String> words= new ArrayList<>();
		
		for (int i=0;i<n;i++) {
			words.add(i, dictionary.get(i));
		}
		return words;
	}
}
