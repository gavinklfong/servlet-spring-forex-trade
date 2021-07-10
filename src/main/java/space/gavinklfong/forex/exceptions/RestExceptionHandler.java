package space.gavinklfong.forex.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {
	
	private static Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);
	
	@ExceptionHandler({InvalidRequestException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorBody handleInvalidRequestException(InvalidRequestException ex) {
	
		List<ObjectError> errors = ex.getErrors();
		List<ErrorMessage> errorMessages = errors.stream()
		.map(error -> { 
			if (error instanceof FieldError) {
				FieldError fieldError = (FieldError) error;
				return new ErrorMessage(fieldError.getField(), fieldError.getDefaultMessage());			
			} else {
				return new ErrorMessage(error.getCode(), error.getDefaultMessage());							
			}
		})
		.collect(Collectors.toList());
		
		return new ErrorBody(errorMessages);
	}
	
	@ExceptionHandler({WebExchangeBindException.class})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorBody handleWebExchangeBindException(WebExchangeBindException ex) {
		
		List<ObjectError> errors = ex.getAllErrors();
		List<ErrorMessage> errorMessages = errors.stream()
		.map(error -> { 
			if (error instanceof FieldError) {
				FieldError fieldError = (FieldError) error;
				return new ErrorMessage(fieldError.getField(), fieldError.getDefaultMessage());			
			} else {
				return new ErrorMessage(error.getCode(), error.getDefaultMessage());							
			}
		})
		.collect(Collectors.toList());
		
		return new ErrorBody(errorMessages);
	}
	
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ErrorBody handleInternalServerError(Exception ex) {
		
		List<ErrorMessage> errorMessages = Arrays.asList(new ErrorMessage("exception", ex.getMessage()));
		
		return new ErrorBody(errorMessages);
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ResponseBody
	public ErrorBody handleNotFound(Exception ex) {
		
		List<ErrorMessage> errorMessages = Arrays.asList(new ErrorMessage("exception", "Resource Not Found"));

		return new ErrorBody(errorMessages);
	}

}
