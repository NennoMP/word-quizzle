package Exceptions;

public class AlreadyLoggedIn extends RuntimeException {
	public AlreadyLoggedIn (String s) {
		super(s);
	}
}
