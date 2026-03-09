package uk.gov.moj.cpp.systemidmapper.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isCustomHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.adapter.rest.exception.ConflictedResourceException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.justice.systemid.mapper.Systemid;
import uk.gov.justice.systemid.mapper.SystemidMapList;
import uk.gov.justice.systemid.mapper.SystemidMappingList;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.MappingResponse;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.service.SystemIdMappingService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemIdMapperApiTest {

    @Mock
    private SystemIdMappingService systemIdMappingService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    private PayloadExtractor payloadExtractor = new PayloadExtractor();

    @InjectMocks
    private SystemIdMapperApi systemIdMapperApi;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Test
    public void shouldHaveCorrectHandlerMethods() throws Exception {
        assertThat(systemIdMapperApi, isCustomHandler("SystemId.Mapping.API")
                .with(method("mapSystemId").thatHandles("systemid.map"))
                .with(method("getSystemIdMapping").thatHandles("systemid.get-mapping"))
                .with(method("findSystemIdMapping").thatHandles("systemid.find-mapping")));
    }

    @Test
    public void shouldGetSystemIdMapping() {
        final UUID mappingId = randomUUID();
        final UUID targetId = randomUUID();
        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.get-mapping"))
                .withPayloadOf(mappingId, "mappingId")
                .build();
        final SystemIdMapping mapping = new SystemIdMapping(mappingId, "sourceId", "sourceType", targetId, "targetType", createdAt);

        when(systemIdMappingService.findMapping(mappingId)).thenReturn(of(mapping));

        final JsonEnvelope systemIdMapping = systemIdMapperApi.getSystemIdMapping(envelope);

        assertThat(systemIdMapping, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.mappingId", equalTo(mappingId.toString())),
                        withJsonPath("$.sourceId", equalTo("sourceId")),
                        withJsonPath("$.sourceType", equalTo("sourceType")),
                        withJsonPath("$.targetId", equalTo(targetId.toString())),
                        withJsonPath("$.targetType", equalTo("targetType")),
                        withJsonPath("$.createdAt", equalTo(ZonedDateTimes.toString(createdAt)))
                ))
                )
        ));
    }

    @Test
    public void shouldReturnNullPayloadFromGetMappingWhenMappingNotFound() {
        final UUID mappingId = randomUUID();
        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.get-mapping"))
                .withPayloadOf(mappingId, "mappingId")
                .build();

        when(systemIdMappingService.findMapping(mappingId)).thenReturn(Optional.empty());

        final JsonEnvelope systemIdMapping = systemIdMapperApi.getSystemIdMapping(envelope);

        assertThat(systemIdMapping.metadata(), is(JsonEnvelopeMetadataMatcher.metadata().withName("systemid.mapping")));
        assertThat(systemIdMapping.payload(), is(JsonValue.NULL));
    }

    @Test
    public void shouldFindSystemIdMapping() {
        final UUID mappingId = randomUUID();
        final UUID targetId = randomUUID();
        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mapping"))
                .withPayloadOf("sourceId", "sourceId")
                .withPayloadOf("sourceType", "sourceType")
                .withPayloadOf("targetType", "targetType")
                .build();
        final SystemIdMapping mapping = new SystemIdMapping(mappingId, "sourceId", "sourceType", targetId, "targetType", createdAt);

        when(systemIdMappingService.findMapping("sourceId", "sourceType", "targetType")).thenReturn(of(mapping));

        final JsonEnvelope systemIdMapping = systemIdMapperApi.findSystemIdMapping(envelope);

        assertThat(systemIdMapping, jsonEnvelope(
                metadata()
                        .withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.mappingId", equalTo(mappingId.toString())),
                        withJsonPath("$.sourceId", equalTo("sourceId")),
                        withJsonPath("$.sourceType", equalTo("sourceType")),
                        withJsonPath("$.targetId", equalTo(targetId.toString())),
                        withJsonPath("$.targetType", equalTo("targetType")),
                        withJsonPath("$.createdAt", equalTo(ZonedDateTimes.toString(createdAt)))
                ))
                )
        );
    }

    @Test
    public void shouldFindSystemIdMappingByTargetIdAndType() {
        final UUID mappingId = randomUUID();
        final UUID targetId = randomUUID();
        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mapping"))
                .withPayloadOf(targetId, "targetId")
                .withPayloadOf("targetType", "targetType")
                .build();
        final SystemIdMapping mapping = new SystemIdMapping(mappingId, "sourceId", "sourceType", targetId, "targetType", createdAt);

        when(systemIdMappingService.findMapping(targetId, "targetType")).thenReturn(of(mapping));

        final JsonEnvelope systemIdMapping = systemIdMapperApi.findSystemIdMapping(envelope);

        assertThat(systemIdMapping, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.mappingId", equalTo(mappingId.toString())),
                        withJsonPath("$.sourceId", equalTo("sourceId")),
                        withJsonPath("$.sourceType", equalTo("sourceType")),
                        withJsonPath("$.targetId", equalTo(targetId.toString())),
                        withJsonPath("$.targetType", equalTo("targetType")),
                        withJsonPath("$.createdAt", equalTo(ZonedDateTimes.toString(createdAt)))
                ))
                )
        ));
    }

    @Test
    public void shouldReturnNullPayloadFromFindMappingWhenMappingNotFound() {
        final UUID targetId = randomUUID();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mapping"))
                .withPayloadOf(targetId, "targetId")
                .withPayloadOf("targetType", "targetType")
                .build();

        when(systemIdMappingService.findMapping(targetId, "targetType")).thenReturn(Optional.empty());

        final JsonEnvelope systemIdMapping = systemIdMapperApi.findSystemIdMapping(envelope);

        assertThat(systemIdMapping.metadata(), is(JsonEnvelopeMetadataMatcher.metadata().withName("systemid.mapping")));
        assertThat(systemIdMapping.payload(), is(JsonValue.NULL));
    }

    @Test
    public void shouldInsertNewMapping() {
        final UUID mappingId = randomUUID();
        final UUID targetId = randomUUID();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.map"))
                .withPayloadOf("sourceId", "sourceId")
                .withPayloadOf("sourceType", "sourceType")
                .withPayloadOf("targetType", "targetType")
                .withPayloadOf(targetId, "targetId")
                .build();

        MappingResponse response = mock(MappingResponse.class);
        when(response.getMappingId()).thenReturn(mappingId);
        when(response.isConflict()).thenReturn(false);

        when(systemIdMappingService.insertMapping("sourceId", "sourceType", targetId, "targetType")).thenReturn(response);

        final JsonEnvelope systemIdMapping = systemIdMapperApi.mapSystemId(envelope);

        assertThat(systemIdMapping, is(jsonEnvelope(
                metadata()
                        .withName("systemid.map"),
                payload().isJson(
                        withJsonPath("$.id", equalTo(mappingId.toString()))
                ))
        ));
    }

    @Test
    public void shouldThrowConflictExceptionOnInsertExistingMapping() {
        final UUID targetId = randomUUID();


        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.map"))
                .withPayloadOf("sourceId", "sourceId")
                .withPayloadOf("sourceType", "sourceType")
                .withPayloadOf("targetType", "targetType")
                .withPayloadOf(targetId, "targetId")
                .build();

        MappingResponse response = mock(MappingResponse.class);
        when(response.isConflict()).thenReturn(true);

        when(systemIdMappingService.insertMapping("sourceId", "sourceType", targetId, "targetType")).thenReturn(response);

        final ConflictedResourceException conflictedResourceException = assertThrows(
                ConflictedResourceException.class,
                () -> systemIdMapperApi.mapSystemId(envelope));


        assertThat(conflictedResourceException.getMessage(), is(format("Insert of mapping sourceId:sourceType to '%s:targetType' failed due to conflict.", targetId)));
    }

    @Test
    public void shouldFindSystemIdMappingBySourceIdAndTargetType() {
        final UUID mappingId = randomUUID();
        final UUID targetId = randomUUID();
        final String sourceId = "GAFTL00:C2AAACD3455";
        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mapping"))
                .withPayloadOf(sourceId, "sourceId")
                .withPayloadOf("targetType1,targetType2", "targetType")
                .build();
        final SystemIdMapping mapping = new SystemIdMapping(mappingId, sourceId, "sourceType", targetId, "targetType1", createdAt);

        when(systemIdMappingService.findMappingBySourceIdAndTargetType(sourceId, "targetType1", "targetType2")).thenReturn(of(mapping));

        final JsonEnvelope systemIdMapping = systemIdMapperApi.findSystemIdMapping(envelope);

        assertThat(systemIdMapping, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.mappingId", equalTo(mappingId.toString())),
                        withJsonPath("$.sourceId", equalTo(sourceId)),
                        withJsonPath("$.sourceType", equalTo("sourceType")),
                        withJsonPath("$.targetId", equalTo(targetId.toString())),
                        withJsonPath("$.targetType", equalTo("targetType1")),
                        withJsonPath("$.createdAt", equalTo(ZonedDateTimes.toString(createdAt)))
                ))
                )
        ));
    }

    @Test
    public void shouldRemapTheSourceId() {
        final UUID mappingId = randomUUID();
        final String newSourceId = "GAFTL00:C2AAACD3455";

        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.remap"))
                .withPayloadOf(mappingId, "mappingId")
                .withPayloadOf(newSourceId, "newSourceId")
                .build();
        final SystemIdMapping newSystemIdMapping = new SystemIdMapping(
                mappingId,
                newSourceId,
                "sourceType",
                randomUUID(),
                "targetType",
                createdAt);

        when(systemIdMappingService.remap(newSourceId, mappingId)).thenReturn(of(newSystemIdMapping));

        final JsonEnvelope systemIdMapping = systemIdMapperApi.remap(envelope);

        assertThat(systemIdMapping, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.mappingId", equalTo(mappingId.toString())),
                        withJsonPath("$.sourceId", equalTo(newSourceId)),
                        withJsonPath("$.sourceType", equalTo(newSystemIdMapping.getSourceType())),
                        withJsonPath("$.targetId", equalTo(newSystemIdMapping.getTargetId().toString())),
                        withJsonPath("$.targetType", equalTo(newSystemIdMapping.getTargetType())),
                        withJsonPath("$.createdAt", equalTo(ZonedDateTimes.toString(createdAt)))
                )))
        ));
    }

    @Test
    public void shouldFailToRemapIfNoMappingFound() {
        final UUID mappingId = fromString("9e29cadb-4afa-4c34-bd6c-58ef43cdc72c");
        final String newSourceId = "GAFTL00:C2AAACD3455";

        final ZonedDateTime createdAt = new UtcClock().now();

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.remap"))
                .withPayloadOf(mappingId, "mappingId")
                .withPayloadOf(newSourceId, "newSourceId")
                .build();

        when(systemIdMappingService.remap(newSourceId, mappingId)).thenReturn(empty());

        try {
            systemIdMapperApi.remap(envelope);
            fail();
        } catch (final BadRequestException expected) {
            assertThat(expected.getMessage(), is("No mapping found for id '9e29cadb-4afa-4c34-bd6c-58ef43cdc72c'"));
        }
    }

    @Test
    public void shouldInsertNewMappings() {
        MappingResponse response = mock(MappingResponse.class);

        final JsonObject systemidMapJson = FileUtil.givenPayload("/test-data/systemid.map.list.json");
        final SystemidMapList systemidMapList = jsonObjectToObjectConverter.convert(systemidMapJson, SystemidMapList.class);
        final Metadata metadata = metadataBuilder().withName("systemid.map.list").withId(randomUUID()).build();
        final Envelope<SystemidMapList> systemIdMapListEnvelope = envelopeFrom(metadata, systemidMapList);

        systemidMapList.getSystemIds().stream().forEach(systemId -> {
            when(response.getMappingId()).thenReturn(randomUUID());
            when(response.isConflict()).thenReturn(false);
            when(systemIdMappingService.insertMapping(systemId.getSourceId(), systemId.getSourceType(), systemId.getTargetId(), systemId.getTargetType())).thenReturn(response);
        });

        final JsonEnvelope systemIdMappingsResponse = systemIdMapperApi.mapSystemIds(systemIdMapListEnvelope);
        final SystemidMappingList systemidMappingList = jsonObjectToObjectConverter.convert(systemIdMappingsResponse.payloadAsJsonObject(), SystemidMappingList.class);
        assertThat(systemidMappingList.getSystemIdMappings().size(), is(2));
        assertThat(systemIdMappingsResponse, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping.list"),
                payload().isJson(allOf(
                        withJsonPath("$.systemIdMappings[*].mappingId", hasSize(2)),
                        withJsonPath("$.systemIdMappings[1].isError", is(false))
                )))
        ));
    }

    @Test
    public void shouldInsertNewMappingsWithExisting() {
        MappingResponse response = mock(MappingResponse.class);
        final JsonObject systemidMapJson = FileUtil.givenPayload("/test-data/systemid.map.list-conflict.json");
        final SystemidMapList systemidMapList = jsonObjectToObjectConverter.convert(systemidMapJson, SystemidMapList.class);
        final Metadata metadata = metadataBuilder().withName("systemid.map.list").withId(randomUUID()).build();
        final Envelope<SystemidMapList> systemIdMapListEnvelope = envelopeFrom(metadata, systemidMapList);
        final Systemid existing = systemidMapList.getSystemIds().get(0);

        MappingResponse errorResponse = mock(MappingResponse.class);
        SystemIdMapping existingSystemIdMapping = new SystemIdMapping(randomUUID(), existing.getSourceId(), existing.getSourceType(), existing.getTargetId(), existing.getTargetType(), ZonedDateTime.now());
        when(errorResponse.getMappingId()).thenReturn(existingSystemIdMapping.getId());
        when(errorResponse.isConflict()).thenReturn(true);
        when(systemIdMappingService.insertMapping(existing.getSourceId(), existing.getSourceType(), existing.getTargetId(), existing.getTargetType())).thenReturn(errorResponse);
        when(systemIdMappingService.findMapping(errorResponse.getMappingId())).thenReturn(Optional.of(existingSystemIdMapping));

        final JsonEnvelope systemIdMappingsConflictResponse = systemIdMapperApi.mapSystemIds(systemIdMapListEnvelope);
        final SystemidMappingList systemidMappingConflictList = jsonObjectToObjectConverter.convert(systemIdMappingsConflictResponse.payloadAsJsonObject(), SystemidMappingList.class);
        assertThat(systemidMappingConflictList.getSystemIdMappings().size(), is(1));
        assertThat(systemIdMappingsConflictResponse, is(jsonEnvelope(
                metadata()
                        .withName("systemid.mapping.list"),
                payload().isJson(allOf(
                        withJsonPath("$.systemIdMappings[*].mappingId", hasSize(1)),
                        withJsonPath("$.systemIdMappings[0].isError", is(false))
                )))
        ));
    }

    @Test
    void shouldFindSystemIdMappingsInBulkBySourceIds() {
        // given
        final UUID mappingId1 = randomUUID();
        final UUID targetId1 = randomUUID();
        final UUID mappingId2 = randomUUID();
        final UUID targetId2 = randomUUID();
        final ZonedDateTime createdAt = ZonedDateTime.now();

        final String sourceIds = "sourceA, sourceB";
        final String targetType = "targetType";

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mappings-bulk"))
                .withPayloadOf(sourceIds, "sourceIds")
                .withPayloadOf(targetType, "targetType")
                .build();

        final SystemIdMapping m1 = new SystemIdMapping(mappingId1, "sourceA", "sourceType",
                targetId1, targetType, createdAt);
        final SystemIdMapping m2 = new SystemIdMapping(mappingId2, "sourceB", "sourceType",
                targetId2, targetType, createdAt);

        when(systemIdMappingService.findMappingsBySourceIdsAndTargetType(
                Optional.of(sourceIds), targetType)
        ).thenReturn(List.of(m1, m2));

        // when
        final JsonEnvelope result = systemIdMapperApi.findSystemIdMappingsInBulk(envelope);

        // then
        assertThat(result, jsonEnvelope(
                metadata().withName("systemid.mapping"),
                payload().isJson(allOf(
                       withJsonPath("$.systemIds[0].sourceId", is("sourceA")),
                       withJsonPath("$.systemIds[1].sourceId", is("sourceB")),
                       withJsonPath("$.systemIds[0].targetType", is("targetType")),
                       withJsonPath("$.systemIds[1].targetType", is("targetType"))
                ))
        ));


        verify(systemIdMappingService).findMappingsBySourceIdsAndTargetType(Optional.of(sourceIds), targetType);
        verifyNoMoreInteractions(systemIdMappingService);
    }

    @Test
    void shouldFindSystemIdMappingsInBulkByTargetIds() {
        // given
        final UUID mappingId1 = randomUUID();
        final UUID mappingId2 = randomUUID();
        final String sourceId1 = "sourceId1";
        final String sourceId2 = "sourceId2";
        String targetId1 = UUID.randomUUID().toString();
        String targetId2 = UUID.randomUUID().toString();
        final ZonedDateTime createdAt = ZonedDateTime.now();

        final String targetIds = String.join(",", targetId1, targetId2);
        final String targetType = "targetType";

        final JsonEnvelope envelope = envelope()
                .with(metadataOf(randomUUID(), "systemid.find-mappings-bulk"))
                .withPayloadOf(targetIds, "targetIds")
                .withPayloadOf(targetType, "targetType")
                .build();

        final SystemIdMapping m1 = new SystemIdMapping(mappingId1, sourceId1, "sourceType",
                UUID.fromString(targetId1), targetType, createdAt);
        final SystemIdMapping m2 = new SystemIdMapping(mappingId2, sourceId2, "sourceType",
                UUID.fromString(targetId2), targetType, createdAt);

        when(systemIdMappingService.findMappingsByTargetIdsAndTargetType(
                Optional.of(targetIds), targetType)
        ).thenReturn(List.of(m1, m2));

        // when
        final JsonEnvelope result = systemIdMapperApi.findSystemIdMappingsInBulk(envelope);

        // then
        assertThat(result, jsonEnvelope(
                metadata().withName("systemid.mapping"),
                payload().isJson(allOf(
                        withJsonPath("$.systemIds[0].targetId", is(targetId1)),
                        withJsonPath("$.systemIds[1].targetId", is(targetId2)),
                        withJsonPath("$.systemIds[0].targetType", is("targetType")),
                        withJsonPath("$.systemIds[1].targetType", is("targetType"))
                ))
        ));

        verify(systemIdMappingService).findMappingsByTargetIdsAndTargetType(Optional.of(targetIds), targetType);
        verifyNoMoreInteractions(systemIdMappingService);
    }

}