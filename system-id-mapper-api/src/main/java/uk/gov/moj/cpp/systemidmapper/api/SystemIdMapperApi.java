package uk.gov.moj.cpp.systemidmapper.api;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.adapter.rest.exception.ConflictedResourceException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.CustomServiceComponent;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.systemid.mapper.SystemIdMappings;
import uk.gov.justice.systemid.mapper.SystemidMapList;
import uk.gov.justice.systemid.mapper.SystemidMappingList;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.MappingResponse;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.service.SystemIdMappingService;

import java.util.*;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CustomServiceComponent("SystemId.Mapping.API")
public class SystemIdMapperApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemIdMapperApi.class);
    private static final String ID = "id";
    private static final String MAPPING_ID = "mappingId";
    private static final String SOURCE_ID = "sourceId";
    private static final String SOURCE_IDS = "sourceIds";
    private static final String SOURCE_TYPE = "sourceType";
    private static final String TARGET_ID = "targetId";
    private static final String TARGET_IDS = "targetIds";
    private static final String TARGET_TYPE = "targetType";
    private static final String CREATED_AT = "createdAt";
    private static final String COMMA = ",";

    @Inject
    private SystemIdMappingService systemIdMappingService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private PayloadExtractor payloadExtractor;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("systemid.map")
    public JsonEnvelope mapSystemId(final JsonEnvelope envelope) {

        final UUID targetId = fromString(envelope.payloadAsJsonObject().getString(TARGET_ID));
        final String sourceId = envelope.payloadAsJsonObject().getString(SOURCE_ID);
        final String sourceType = envelope.payloadAsJsonObject().getString(SOURCE_TYPE);
        final String targetType = envelope.payloadAsJsonObject().getString(TARGET_TYPE);

        final MappingResponse mappingResponse = systemIdMappingService.insertMapping(sourceId, sourceType, targetId, targetType);

        if (mappingResponse.isConflict()) {
            throw new ConflictedResourceException(format("Insert of mapping %s:%s to '%s:%s' failed due to conflict.", sourceId, sourceType, targetId, targetType), mappingResponse.getMappingId());
        }

        return enveloper.withMetadataFrom(envelope, "systemid.map").apply(payloadFrom(mappingResponse.getMappingId()));
    }

    @Handles("systemid.get-mapping")
    public JsonEnvelope getSystemIdMapping(final JsonEnvelope envelope) {

        final UUID mappingId = fromString(envelope.payloadAsJsonObject().getString(MAPPING_ID));

        final Optional<SystemIdMapping> mapping = systemIdMappingService.findMapping(mappingId);

        return envelopeFor(envelope, mapping);
    }

    @Handles("systemid.find-mapping")
    public JsonEnvelope findSystemIdMapping(final JsonEnvelope envelope) {

        final JsonObject payloadAsJsonObject = payloadExtractor.extractPayloadOrThrowBadRequestException(envelope);

        final String targetType = payloadAsJsonObject.getString(TARGET_TYPE);

        Optional<SystemIdMapping> mapping;
        if (payloadAsJsonObject.containsKey(TARGET_ID) && payloadAsJsonObject.containsKey(TARGET_TYPE) &&
                !payloadAsJsonObject.containsKey(SOURCE_TYPE)) {
            final UUID targetId = fromString(payloadAsJsonObject.getString(TARGET_ID));
            mapping = systemIdMappingService.findMapping(targetId, targetType);
        } else if (payloadAsJsonObject.containsKey(SOURCE_ID) && payloadAsJsonObject.containsKey(TARGET_TYPE) &&
                !payloadAsJsonObject.containsKey(SOURCE_TYPE)) {
            mapping = systemIdMappingService.findMappingBySourceIdAndTargetType(payloadAsJsonObject.getString(SOURCE_ID), targetType.split(COMMA));
        } else {
            final String sourceId = payloadAsJsonObject.getString(SOURCE_ID);
            final String sourceType = payloadAsJsonObject.getString(SOURCE_TYPE);
            mapping = systemIdMappingService.findMapping(sourceId, sourceType, targetType);
        }
        return envelopeFor(envelope, mapping);
    }

    @Handles("systemid.find-mappings-bulk")
    public JsonEnvelope findSystemIdMappingsInBulk(final JsonEnvelope envelope) {

        final JsonObject payload = payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(envelope);

        final String targetType = payload.getString(TARGET_TYPE);

        List<SystemIdMapping> mappings = List.of();
        if (payload.containsKey(SOURCE_IDS)) {
            final String sourceIds = payload.getString(SOURCE_IDS);
            mappings = systemIdMappingService.findMappingsBySourceIdsAndTargetType(Optional.ofNullable(sourceIds), targetType);
        } else if (payload.containsKey(TARGET_IDS)) {
            final String targetIds = payload.getString(TARGET_IDS);
            mappings = systemIdMappingService.findMappingsByTargetIdsAndTargetType(Optional.ofNullable(targetIds), targetType);
        }

        return envelopeForBulk(envelope, mappings);
    }

    private JsonEnvelope envelopeForBulk(final JsonEnvelope originalEnvelope, final List<SystemIdMapping> mappings) {
        final List<JsonObject> jsonObjects = mappings.stream()
                .map(this::payloadFrom)
                .toList();
        final Map<String, List<JsonObject>> map = new HashMap<>();
        map.put("systemIds", jsonObjects);
        return enveloper.withMetadataFrom(originalEnvelope, "systemid.mapping").apply(map);
    }

    @Handles("systemid.remap")
    public JsonEnvelope remap(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final String newSourceId = payload.getString("newSourceId");
        final String mappingId = payload.getString(MAPPING_ID);

        final Optional<SystemIdMapping> systemIdMapping = systemIdMappingService.remap(newSourceId, fromString(mappingId));

        if (systemIdMapping.isPresent()) {
            return envelopeFor(envelope, systemIdMapping);
        }

        throw new BadRequestException(format("No mapping found for id '%s'", mappingId));
    }

    private JsonObject payloadFrom(final UUID mappingId) {
        return JsonObjects.createObjectBuilder()
                .add(ID, mappingId.toString())
                .build();
    }

    private JsonObject payloadFrom(final SystemIdMapping mapping) {
        return JsonObjects.createObjectBuilder()
                .add(MAPPING_ID, mapping.getId().toString())
                .add(SOURCE_ID, mapping.getSourceId())
                .add(SOURCE_TYPE, mapping.getSourceType())
                .add(TARGET_ID, mapping.getTargetId().toString())
                .add(TARGET_TYPE, mapping.getTargetType())
                .add(CREATED_AT, ZonedDateTimes.toString(mapping.getCreatedAt()))
                .build();
    }

    private JsonEnvelope envelopeFor(final JsonEnvelope originalEnvelope, Optional<SystemIdMapping> mapping) {
        return enveloper.withMetadataFrom(originalEnvelope, "systemid.mapping").apply(mapping.map(this::payloadFrom).orElse(null));
    }

    /**
     * Add system id mappings for source and target.
     * Use this API for bulk system id's mappings. ( Ex: Like Group/Civil cases)
     * @param systemIdMapListEnvelope with {@link SystemidMapList} the mappings of multiple source to target ids
     * If existing mapping is found same will be returned
     * @return the JsonEnvelope with {@link SystemidMappingList} that represents the response
     */
    @Handles("systemid.map.list")
    public JsonEnvelope mapSystemIds(final Envelope<SystemidMapList> systemIdMapListEnvelope) {
        final SystemidMapList systemidMapList = systemIdMapListEnvelope.payload();
        final List<SystemIdMappings> systemIdMappings = new ArrayList<>();
        systemidMapList.getSystemIds().forEach(systemId -> {
            final MappingResponse mappingResponse = systemIdMappingService.insertMapping(systemId.getSourceId(), systemId.getSourceType(), systemId.getTargetId(), systemId.getTargetType());
            if (mappingResponse.isConflict()) {
                if(nonNull(mappingResponse.getMappingId())) {
                    LOGGER.warn("Existing System-id mapping found for SourceId = {}, SourceType = {}, TargetType= {} - will use existing mapping", systemId.getSourceId(), systemId.getSourceType(), systemId.getTargetType());
                    systemIdMappingService.findMapping(mappingResponse.getMappingId())
                            .ifPresent(systemIdMapping -> systemIdMappings.add(SystemIdMappings.systemIdMappings().withIsError(false)
                                    .withMappingId(systemIdMapping.getId()).withSourceId(systemIdMapping.getSourceId())
                                    .withTargetId(systemIdMapping.getTargetId()).build())
                            );
                } else {
                    LOGGER.error("Error creating System-id mapping for SourceId = {}, SourceType = {}, TargetType= {} - check the mapping", systemId.getSourceId(), systemId.getSourceType(), systemId.getTargetType());
                }
            } else {
                systemIdMappings.add(SystemIdMappings.systemIdMappings().withIsError(false)
                        .withMappingId(mappingResponse.getMappingId()).withSourceId(systemId.getSourceId())
                        .withTargetId(systemId.getTargetId()).build());
            }
        });

        final SystemidMappingList systemidMappingList = SystemidMappingList.systemidMappingList().withSystemIdMappings(systemIdMappings).build();
        final JsonObject resultJson = objectToJsonObjectConverter.convert(systemidMappingList);
        return envelopeFrom(metadataBuilder()
                .withId(systemIdMapListEnvelope.metadata().id())
                .withName("systemid.mapping.list")
                .build(), resultJson);
    }
}
