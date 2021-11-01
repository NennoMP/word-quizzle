package Exceptions;

public class UserAlreadyExists extends RuntimeException {
	public UserAlreadyExists (String s) {
		super(s);
	}
}
