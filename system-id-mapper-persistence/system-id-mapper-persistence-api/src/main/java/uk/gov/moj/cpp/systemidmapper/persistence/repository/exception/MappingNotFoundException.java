package uk.gov.moj.cpp.systemidmapper.persistence.repository.exception;

public class MappingNotFoundException extends RuntimeException {

    public MappingNotFoundException(final String message) {
        super(message);
    }
}
