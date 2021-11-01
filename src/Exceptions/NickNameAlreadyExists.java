package Exceptions;

public class NickNameAlreadyExists extends RuntimeException {
	public NickNameAlreadyExists (String s) {
		super(s);
	}
}
