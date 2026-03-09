package uk.gov.moj.cpp.systemidmapper.api;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

public class PayloadExtractor {

    private static final String SOURCE_ID = "sourceId";
    private static final String SOURCE_IDS = "sourceIds";
    private static final String SOURCE_TYPE = "sourceType";
    private static final String TARGET_ID = "targetId";
    private static final String TARGET_IDS = "targetIds";
    private static final String TARGET_TYPE = "targetType";

    private static final String BAD_REQUEST_ERROR = "Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceId) or (targetType and sourceType) or (targetType and targetId)";
    private static final String BAD_REQUEST_ERROR_BULK = "Bad Request, invalid set of query parameters provided. Please query with either (targetType and sourceIds) or (targetType and targetIds)";

    public JsonObject extractPayloadOrThrowBadRequestException(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        if (payload.containsKey(TARGET_TYPE)
                && (payload.containsKey(SOURCE_ID) || payload.containsKey(SOURCE_TYPE) || payload.containsKey(TARGET_ID))) {
            return payload;
        }

        throw new BadRequestException(BAD_REQUEST_ERROR);
    }

    public JsonObject extractPayloadOrThrowBadRequestExceptionForBulkOperation(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final String targetType = payload.getString(TARGET_TYPE, null);
        final String sourceIds = payload.getString(SOURCE_IDS, null);
        final String targetIds = payload.getString(TARGET_IDS, null);

        if (targetType != null && !targetType.isBlank() && (sourceIds != null ^ targetIds != null)) {
            return payload;
        }

        throw new BadRequestException(BAD_REQUEST_ERROR_BULK);
    }

}
