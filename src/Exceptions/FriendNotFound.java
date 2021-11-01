package Exceptions;

public class FriendNotFound extends RuntimeException {
	public FriendNotFound (String s) {
		super(s);
	}
}
