package Server;

import java.io.FileReader;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/** Classe contenente metodi utili per la gestione di tutte le operazioni relative al file JSON (salvataggio, creazione, lettura) **/

public class JSONUtils {
	
	/* Crea JSON dell'oggetto */
	public static String createJSON (Object o) {
		String json= null;
		try {
			ObjectWriter objWriter= new ObjectMapper().writer().withDefaultPrettyPrinter();
			json= objWriter.writeValueAsString(o);
			System.out.println(json);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	/* Carica la lista utenti letta dal JSON in una HashMap */
	public static ConcurrentHashMap<String,User> readFromJSON (String filename) throws IllegalArgumentException {
		if (filename==null || filename.equals("")) throw new IllegalArgumentException();
		
		JSONParser parserJSON= new JSONParser();
		try (FileReader reader= new FileReader(filename)) {
			 JSONObject objJSON= (JSONObject) parserJSON.parse(reader);
			 JSONArray usersList= (JSONArray) objJSON.get("Users");
			 
			 ConcurrentHashMap<String,User> registeredUsers= new ConcurrentHashMap<String,User>();
			 String username, password;
			 long scoreTOT;
			 for (Object o : usersList) {
				 // registro i vari campi
				 JSONObject tmp= (JSONObject) o;
				 username= (String) tmp.get("UserName");
				 password= (String) tmp.get("password");
				 scoreTOT=  (long) tmp.get("scoreTOT");
				 User user= new User(username,password);
				 user.setTotalScore(scoreTOT);
				 
				 // itero per la lista amici
				 JSONArray friends= (JSONArray) tmp.get("friendsList");
				 Iterator<String> iterator= friends.iterator();
				 while (iterator.hasNext()) {
					 user.addFriend(iterator.next());
				 }
				 // aggiungo l'user letto alla mia lista
				 registeredUsers.put(username, user);
			 }
			return registeredUsers;
		}	
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/* Salva le modifiche alla lista users sul JSON */
	public static void SaveJsonFile () {
		RegisteredUsers iru= RegisteredUsers.getInstance();
		JSONArray result= new JSONArray();

		System.out.println("[SAVING] Saving users file...");
		// itero sugli utenti
		for (String s : iru.getRegisteredUsers().keySet()) {
			User u= iru.getUser(s);
			String username= u.getUserName();
			String password= u.getPassword();
			long scoreTOT= u.getTotalScore();
			JSONObject obj= new JSONObject();
			
			// salvo i campi
			obj.put("UserName", username);
			obj.put("password", password);
			obj.put("scoreTOT", scoreTOT);
			
			// itero sulla lista amici
			JSONArray list= new JSONArray();
			for (String s2 : u.getFriends().keySet()) {
				list.add(s2);
			}
			// creo oggetto user e lo aggiungo alla lista users
			obj.put("friendsList", list);
			result.add(obj);
		}
		// aggiungo lista users all'oggetto finale
		JSONObject parameters= new JSONObject();
		parameters.put("Users", result);
		
		// scrivo sul file
		try (FileWriter file= new FileWriter(ConfigurationSettings.FILE_JSON)) {
			file.write(parameters.toJSONString());
			System.out.println("[SAVING] Completed");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Stampa il JSON della lista amici (CLIENT) */
	public static void printFriendsJSON (String json)  {
		if (json==null || json.equals("")) throw new IllegalArgumentException();
		
		try {
			 ObjectMapper mapper= new ObjectMapper();
			 HashMap<String,String> friendsList= mapper.reader()
					 .withType(new TypeReference<HashMap<String,String>>() {})
					 .readValue(json.getBytes());
			 
			 Iterator<String> iterator= friendsList.values().iterator();
			 StringBuilder toPrint= new StringBuilder("Amici: ");
			 while (iterator.hasNext()) {
				 String friend= iterator.next();
				 toPrint.append((iterator.hasNext()) ? friend + ", " : friend);
				 iterator.remove();
			 }
			 System.out.println(toPrint);
			 
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* Stampa il JSON della classifica (CLIENT) */
	public static void printRankingJSON (ArrayList<String> rankingList)  {
		String prefix= "Classifica: ";
		StringBuilder toPrint = new StringBuilder(prefix + "");
        for (String el : rankingList) toPrint.append(el).append(", ");
        toPrint = new StringBuilder(toPrint.substring(0, toPrint.length() - 2));
        System.out.println(toPrint);
	}
	
}
