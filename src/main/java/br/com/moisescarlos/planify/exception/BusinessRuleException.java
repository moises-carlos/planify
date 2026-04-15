package br.com.moisescarlos.planify.exception;

public class BusinessRuleException extends RuntimeException{
    public BusinessRuleException(String message) {
        super(message);
    }
}
