package uk.gov.moj.cpp.systemidmapper.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.JsonObjects;

@ExtendWith(MockitoExtension.class)
public class PayloadExtractorTest {

    @InjectMocks
    private PayloadExtractor payloadExtractor;

    @Test
    public void shouldReturnExtractedPayloadIfTargetTypeAndSourceIdArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("sourceId", "sourceId")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final JsonObject resultPayload = payloadExtractor.extractPayloadOrThrowBadRequestException(jsonEnvelope);

        assertThat(resultPayload, is(payload));
    }

    @Test
    public void shouldReturnExtractedPayloadIfTargetTypeAndSourceTypeArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("sourceType", "sourceType")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final JsonObject resultPayload = payloadExtractor.extractPayloadOrThrowBadRequestException(jsonEnvelope);

        assertThat(resultPayload, is(payload));
    }

    @Test
    public void shouldReturnExtractedPayloadIfTargetTypeAndTargetIdArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("targetId", "targetId")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final JsonObject resultPayload = payloadExtractor.extractPayloadOrThrowBadRequestException(jsonEnvelope);

        assertThat(resultPayload, is(payload));
    }

    @Test
    public void shouldThrowExceptionIfTargetTypeIsNotPresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetId", "targetId")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestException(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceId) or (targetType and sourceType) or (targetType and targetId)"));
        }
    }

    @Test
    public void shouldThrowExceptionIfTargetTypeIsPresentButSourceIdAndSourceTypeAndTargetIdAreNotPresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestException(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceId) or (targetType and sourceType) or (targetType and targetId)"));
        }
    }

    @Test
    public void shouldReturnExtractedPayloadForBulkOperationIfTargetTypeAndSourceIdsArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("sourceIds", "source1,source2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final JsonObject resultPayload = payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);

        assertThat(resultPayload, is(payload));
    }

    @Test
    public void shouldReturnExtractedPayloadForBulkOperationIfTargetTypeAndTargetIdsArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("targetIds", "target1,target2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        final JsonObject resultPayload = payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);

        assertThat(resultPayload, is(payload));
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfBothSourceIdsAndTargetIdsArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("sourceIds", "source1,source2")
                .add("targetIds", "target1,target2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfNeitherSourceIdsNorTargetIdsArePresent() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetTypeIsJsonObject() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", JsonObjects.createObjectBuilder().build())
                .add("sourceIds", "source1,source2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfSourceIdsIsJsonArray() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .add("sourceIds", javax.json.Json.createArrayBuilder().add("source1").add("source2").build())
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetIdsIsNull() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "targetType")
                .addNull("targetIds")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetTypeIsNumber() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", 123)
                .add("sourceIds", "source1,source2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetTypeIsBoolean() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", true)
                .add("targetIds", "target1,target2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetTypeIsEmpty() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "")
                .add("sourceIds", "source1,source2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

    @Test
    public void shouldThrowExceptionForBulkOperationIfTargetTypeIsBlank() {

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("targetType", "   ")
                .add("targetIds", "target1,target2")
                .build();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        try {
            payloadExtractor.extractPayloadOrThrowBadRequestExceptionForBulkOperation(jsonEnvelope);
            fail();
        } catch (final BadRequestException e) {
            assertThat(e.getMessage(), is("Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)"));
        }
    }

}