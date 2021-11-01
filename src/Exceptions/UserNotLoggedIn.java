package Exceptions;

public class UserNotLoggedIn extends RuntimeException {
	public UserNotLoggedIn (String s) {
		super(s);
	}
}
