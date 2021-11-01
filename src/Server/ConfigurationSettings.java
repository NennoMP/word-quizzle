package Server;

/** CLASSE INFO DI CONFIGURAZIONE **/

public class ConfigurationSettings {
	public static final int N_THREADS = 100;
	
	// ports
	public static final int RMI_PORT= 15000;
	public static final int TCP_PORT= 15001;
	public static final int UDP_PORT= 15002;
	
    // file, API, host
    public static final String FILE_WORDS = "words.txt";
    public static final String FILE_JSON = "WQusers.json";
    public static final String HOST_NAME = "localhost";
    public static final String API_URL = "https://api.mymemory.translated.net/get?langpair=it|en&q=";
    
    // game settings
    public static final int MATCH_WORDS= 5;
    public static final int TIME_GAME = 60; // Seconds
    public static final int TIME_CHALLENGE_REQUEST= 25; // Seconds
    public static final int UDP_TIMEOUT = 25000; // Milliseconds
    public static final int WIN_POINTS= 5;
    public static final int CORRECT_ANSWER_POINTS= 3;
    public static final int WRONG_ANSWER_POINTS= -1;
}