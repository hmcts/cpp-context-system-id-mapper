package uk.gov.moj.cpp.systemidmapper.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.systemidmapper.persistence.repository.api.SystemIdMappingRepository;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.MappingResponse;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.entity.SystemIdMapping;
import uk.gov.moj.cpp.systemidmapper.persistence.repository.exception.MappingConflictException;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class SystemIdMappingServiceTest {

    @Mock
    private SystemIdMappingRepository systemIdMappingRepository;

    @Mock
    private Logger logger;

    @InjectMocks
    private SystemIdMappingService systemIdMappingService;

    @Test
    public void shouldGetMappingById() {
        final UUID mappingId = randomUUID();

        final SystemIdMapping systemIdMapping = mock(SystemIdMapping.class);
        when(systemIdMappingRepository.getSystemIdMapping(mappingId)).thenReturn(of(systemIdMapping));

        final Optional<SystemIdMapping> mapping = systemIdMappingService.findMapping(mappingId);

        assertThat(mapping.get(), is(equalTo(systemIdMapping)));
    }

    @Test
    public void shouldGetMappingByQueryParams() {
        final String sourceId = "sourceId";
        final String sourceType = "sourceType";
        final String targetType = "targetType";

        final SystemIdMapping systemIdMapping = mock(SystemIdMapping.class);
        when(systemIdMappingRepository.findSystemIdMapping(sourceId, sourceType, targetType)).thenReturn(of(systemIdMapping));

        final Optional<SystemIdMapping> mapping = systemIdMappingService.findMapping(sourceId, sourceType, targetType);

        assertThat(mapping.get(), is(equalTo(systemIdMapping)));
    }

    @Test
    public void shouldInsertNewMapping() throws MappingConflictException {
        final String sourceId = "sourceId";
        final String sourceType = "sourceType";
        final String targetType = "targetType";
        final UUID targetId = randomUUID();

        final UUID mappingId = randomUUID();
        when(systemIdMappingRepository.insertSystemIdMapping(sourceId, sourceType, targetId, targetType)).thenReturn(mappingId);

        final MappingResponse mappingResponse = systemIdMappingService.insertMapping(sourceId, sourceType, targetId, targetType);

        assertThat(mappingResponse.getMappingId(), is(equalTo(mappingId)));
        assertThat(mappingResponse.isConflict(), is(false));
    }

    @Test
    public void shouldReturnConflictOnInsertOfExistingMapping() throws MappingConflictException {
        final String sourceId = "sourceId";
        final String sourceType = "sourceType";
        final String targetType = "targetType";
        final UUID targetId = randomUUID();

        final UUID mappingId = randomUUID();
        when(systemIdMappingRepository.insertSystemIdMapping(sourceId, sourceType, targetId, targetType)).thenThrow(new MappingConflictException("Mapping Exists"));
        final SystemIdMapping systemIdMapping = mock(SystemIdMapping.class);
        when(systemIdMappingRepository.findSystemIdMapping(sourceId, sourceType, targetType)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getId()).thenReturn(mappingId);
        when(systemIdMapping.getTargetId()).thenReturn(targetId);

        final MappingResponse mappingResponse = systemIdMappingService.insertMapping(sourceId, sourceType, targetId, targetType);

        assertThat(mappingResponse.getMappingId(), is(equalTo(mappingId)));
        assertThat(mappingResponse.isConflict(), is(equalTo(false)));
    }

    @Test
    public void shouldReturnConflictOnInsertOfExistingDifferentMapping() throws MappingConflictException {
        final String sourceId = "sourceId";
        final String sourceType = "sourceType";
        final String targetType = "targetType";
        final UUID targetId = randomUUID();

        final UUID mappingId = randomUUID();
        when(systemIdMappingRepository.insertSystemIdMapping(sourceId, sourceType, targetId, targetType)).thenThrow(new MappingConflictException("Mapping Exists"));
        final SystemIdMapping systemIdMapping = mock(SystemIdMapping.class);
        when(systemIdMappingRepository.findSystemIdMapping(sourceId, sourceType, targetType)).thenReturn(of(systemIdMapping));
        when(systemIdMapping.getId()).thenReturn(mappingId);
        when(systemIdMapping.getTargetId()).thenReturn(randomUUID());

        final MappingResponse mappingResponse = systemIdMappingService.insertMapping(sourceId, sourceType, targetId, targetType);

        assertThat(mappingResponse.getMappingId(), is(equalTo(mappingId)));
        assertThat(mappingResponse.isConflict(), is(equalTo(true)));
    }

    @Test
    public void shouldGetMappingBySourceIdAndTargetType() {
        final String sourceId = "sourceId";
        final String targetType = "targetType";

        final SystemIdMapping systemIdMapping = mock(SystemIdMapping.class);
        when(systemIdMappingRepository.findBySourceIdAndTargetType(sourceId, targetType)).thenReturn(of(systemIdMapping));

        final Optional<SystemIdMapping> mapping = systemIdMappingService.findMappingBySourceIdAndTargetType(sourceId, targetType);

        assertThat(mapping.get(), is(equalTo(systemIdMapping)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRemapTheSourceId() throws Exception {

        final String newSourceId = "new-source-id";
        final UUID mappingId = randomUUID();

        final Optional<SystemIdMapping> originalSystemIdMapping = of(mock(SystemIdMapping.class));
        final Optional<SystemIdMapping> updatedSystemIdMapping = of(mock(SystemIdMapping.class));
        when(systemIdMappingRepository.getSystemIdMapping(mappingId)).thenReturn(originalSystemIdMapping, updatedSystemIdMapping);

        assertThat(systemIdMappingService.remap(newSourceId, mappingId), is(updatedSystemIdMapping));

        final InOrder inOrder = inOrder(systemIdMappingRepository);

        inOrder.verify(systemIdMappingRepository).getSystemIdMapping(mappingId);
        inOrder.verify(systemIdMappingRepository).remapSystemIdMapping(newSourceId, mappingId);
        inOrder.verify(systemIdMappingRepository).getSystemIdMapping(mappingId);
    }

    @Test
    public void shouldLogErrorAndReturnEmptyIfNoMappingFoundToRemap() throws Exception {

        final String newSourceId = "new-source-id";
        final UUID mappingId = fromString("878b776f-1d53-479e-9ae2-097a67dfbe41");

        when(systemIdMappingRepository.getSystemIdMapping(mappingId)).thenReturn(empty());

        assertThat(systemIdMappingService.remap(newSourceId, mappingId), is(empty()));

        verify(logger).error("Failed to update mapping with mapping_id '878b776f-1d53-479e-9ae2-097a67dfbe41': No mapping found");
    }
}
