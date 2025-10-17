package uk.gov.moj.cpp.systemidmapper.persistence.repository.exception;

public class MappingConflictException extends Exception {

    public MappingConflictException(final String message) {
        super(message);
    }
}
