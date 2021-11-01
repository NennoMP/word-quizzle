package Exceptions;

public class FriendAlreadyPlaying extends RuntimeException {
	public FriendAlreadyPlaying (String s) {
		super(s);
	}
}
