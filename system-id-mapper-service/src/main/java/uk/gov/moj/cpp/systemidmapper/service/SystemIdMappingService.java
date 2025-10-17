package uk.gov.moj.cpp.systemidmapper.service;

import static java.lang.String.format;
import static java.util.Optional.empty;

import uk.gov.moj.cpp.systemidmapper.persistence.repository.api.SystemIdMappingRepository;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.MappingResponse;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

public class SystemIdMappingService {

    @Inject
    private SystemIdMappingRepository systemIdMappingRepository;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @SuppressWarnings("squid:S1166")
    public MappingResponse insertMapping(final String sourceId, final String sourceType, final UUID targetId, final String targetType) {
        try {
            final UUID mappingId = systemIdMappingRepository.insertSystemIdMapping(sourceId, sourceType, targetId, targetType);
            return new MappingResponse(mappingId, false);
        } catch (final MappingConflictException exception) {
            final Optional<SystemIdMapping> systemIdMapping = systemIdMappingRepository.findSystemIdMapping(sourceId, sourceType, targetType);
            return systemIdMapping
                    .map(idMapping -> new MappingResponse(idMapping.getId(), !targetId.equals(idMapping.getTargetId())))
                    .orElseGet(() -> new MappingResponse(null, true));
        }
    }

    public Optional<SystemIdMapping> findMapping(final UUID mappingId) {
        return systemIdMappingRepository.getSystemIdMapping(mappingId);
    }

    public Optional<SystemIdMapping> findMapping(final String sourceId, final String sourceType, final String targetType) {
        return systemIdMappingRepository.findSystemIdMapping(sourceId, sourceType, targetType);
    }

    public Optional<SystemIdMapping> findMapping(final UUID targetId, final String targetType) {
        return systemIdMappingRepository.findSystemIdMapping(targetId, targetType);
    }

    public Optional<SystemIdMapping> findMappingBySourceIdAndTargetType(final String sourceId, final String... targetTypes) {
        return systemIdMappingRepository.findBySourceIdAndTargetType(sourceId, targetTypes);
    }

    @SuppressWarnings("squid:S2629")
    public Optional<SystemIdMapping> remap(final String newSourceId, final UUID mappingId) {

        final Optional<SystemIdMapping> systemIdMappingOptional = systemIdMappingRepository.getSystemIdMapping(mappingId);

        if (systemIdMappingOptional.isPresent()) {
            systemIdMappingRepository.remapSystemIdMapping(newSourceId, mappingId);

            return systemIdMappingRepository.getSystemIdMapping(mappingId);
        }

        logger.error(format("Failed to update mapping with mapping_id '%s': No mapping found", mappingId));

        return empty();
    }
}
