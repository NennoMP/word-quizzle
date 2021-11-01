package Exceptions;

public class FriendAlreadyExists extends RuntimeException {
	public FriendAlreadyExists (String s) {
		super(s);
	}
}
