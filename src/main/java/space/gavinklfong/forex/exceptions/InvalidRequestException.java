package space.gavinklfong.forex.exceptions;

import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;

public class InvalidRequestException extends RuntimeException {

	protected List<ObjectError> errors;
	
	public InvalidRequestException() {
		super();
	}
	
	public InvalidRequestException(List<ObjectError> errors) {
		super();
		this.errors = errors;
	}	

	public InvalidRequestException(String name, String message) {
		super();
		this.errors = new ArrayList<>();
		this.errors.add(new ObjectError(name, message));
	}	
	
	public List<ObjectError> getErrors() {
		return errors;
	}
	

}
