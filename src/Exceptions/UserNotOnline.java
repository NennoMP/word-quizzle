package Exceptions;

public class UserNotOnline extends RuntimeException {
	public UserNotOnline (String s) {
		super(s);
	}

}
