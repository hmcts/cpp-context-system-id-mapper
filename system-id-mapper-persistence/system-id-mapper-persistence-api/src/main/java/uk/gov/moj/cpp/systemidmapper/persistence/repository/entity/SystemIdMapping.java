package uk.gov.moj.cpp.systemidmapper.persistence.repository.entity;


import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class SystemIdMapping {

    private final UUID id;
    private final String sourceId;
    private final String sourceType;
    private final UUID targetId;
    private final String targetType;
    private final ZonedDateTime createdAt;

    public SystemIdMapping(final UUID id, final String sourceId, final String sourceType, final UUID targetId, final String targetType, final ZonedDateTime createdAt) {
        this.id = id;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SystemIdMapping that = (SystemIdMapping) o;
        return Objects.equals(getId(), that.getId()) &&
                Objects.equals(getSourceId(), that.getSourceId()) &&
                Objects.equals(getSourceType(), that.getSourceType()) &&
                Objects.equals(getTargetId(), that.getTargetId()) &&
                Objects.equals(getTargetType(), that.getTargetType()) &&
                Objects.equals(getCreatedAt(), that.getCreatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSourceId(), getSourceType(), getTargetId(), getTargetType(), getCreatedAt());
    }
}
