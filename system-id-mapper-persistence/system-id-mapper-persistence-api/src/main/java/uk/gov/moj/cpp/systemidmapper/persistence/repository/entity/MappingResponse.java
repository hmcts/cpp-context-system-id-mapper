package uk.gov.moj.cpp.systemidmapper.persistence.repository.entity;

import java.util.UUID;

public class MappingResponse {

    private final UUID mappingId;
    private final boolean conflict;

    public MappingResponse(final UUID mappingId, final boolean conflict) {
        this.mappingId = mappingId;
        this.conflict = conflict;
    }

    public UUID getMappingId() {
        return mappingId;
    }

    public boolean isConflict() {
        return conflict;
    }
}
