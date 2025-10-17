package uk.gov.moj.cpp.systemidmapper.persistence.repository.api;


import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;

import java.util.Optional;
import java.util.UUID;

public interface SystemIdMappingRepository {

    Optional<SystemIdMapping> getSystemIdMapping(final UUID mappingId);

    Optional<SystemIdMapping> findSystemIdMapping(final String sourceId, final String sourceType, final String targetType);

    Optional<SystemIdMapping> findSystemIdMapping(final UUID targetId, final String targetType);

    Optional<SystemIdMapping> findBySourceIdAndTargetType(final String sourceId, final String... targetTypes);

    UUID insertSystemIdMapping(final String sourceId, final String sourceType, final UUID targetId, final String targetType) throws MappingConflictException;

    void remapSystemIdMapping(final String newSourceId, final UUID mappingId);
}
