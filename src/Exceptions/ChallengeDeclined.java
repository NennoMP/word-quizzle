package Exceptions;

public class ChallengeDeclined extends RuntimeException {
	public ChallengeDeclined (String s) {
		super(s);
	}
}
