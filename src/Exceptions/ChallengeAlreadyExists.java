package Exceptions;

public class ChallengeAlreadyExists extends RuntimeException {
	public ChallengeAlreadyExists (String s) {
		super(s);
	}
}
