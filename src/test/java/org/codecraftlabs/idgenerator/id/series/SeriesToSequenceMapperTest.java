package org.codecraftlabs.idgenerator.id.series;

import org.codecraftlabs.idgenerator.id.repository.DatabaseException;
import org.codecraftlabs.idgenerator.id.repository.SeriesDetail;
import org.codecraftlabs.idgenerator.id.repository.UniqueIdMapperRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeriesToSequenceMapperTest {
    @Mock
    private UniqueIdMapperRepository uniqueIdMapperRepository;

    @InjectMocks
    private SeriesToSequenceMapper seriesToSequenceMapper;

    @Test
    void should_load_series_details_from_repository_on_cache_miss() {
        when(uniqueIdMapperRepository.getSeriesDetails("orders")).thenReturn(new SeriesDetail("idfs_orders", "ORD"));

        assertThat(seriesToSequenceMapper.getSeriesDetails("orders"))
                .contains(new SeriesInfo("idfs_orders", "ORD"));
    }

    @Test
    void should_return_empty_and_not_cache_when_series_is_not_found() {
        when(uniqueIdMapperRepository.getSeriesDetails("unknown")).thenReturn(null);

        assertThat(seriesToSequenceMapper.getSeriesDetails("unknown")).isEmpty();
        assertThat(seriesToSequenceMapper.getSeriesDetails("unknown")).isEmpty();
        verify(uniqueIdMapperRepository, times(2)).getSeriesDetails("unknown");
    }

    @Test
    void should_return_cached_series_details_without_querying_repository_again() {
        when(uniqueIdMapperRepository.getSeriesDetails("orders")).thenReturn(new SeriesDetail("idfs_orders", "ORD"));

        var first = seriesToSequenceMapper.getSeriesDetails("orders");
        var second = seriesToSequenceMapper.getSeriesDetails("orders");

        assertThat(second).containsSame(first.orElseThrow());
        verify(uniqueIdMapperRepository, times(1)).getSeriesDetails("orders");
    }

    @Test
    void should_cache_each_series_independently() {
        when(uniqueIdMapperRepository.getSeriesDetails("orders")).thenReturn(new SeriesDetail("idfs_orders", "ORD"));
        when(uniqueIdMapperRepository.getSeriesDetails("product")).thenReturn(new SeriesDetail("idfs_product", "PRD"));

        assertThat(seriesToSequenceMapper.getSeriesDetails("orders")).contains(new SeriesInfo("idfs_orders", "ORD"));
        assertThat(seriesToSequenceMapper.getSeriesDetails("product")).contains(new SeriesInfo("idfs_product", "PRD"));
        assertThat(seriesToSequenceMapper.getSeriesDetails("orders")).contains(new SeriesInfo("idfs_orders", "ORD"));

        verify(uniqueIdMapperRepository, times(1)).getSeriesDetails("orders");
        verify(uniqueIdMapperRepository, times(1)).getSeriesDetails("product");
    }

    @Test
    void should_propagate_database_exception_and_not_cache_on_failure() {
        when(uniqueIdMapperRepository.getSeriesDetails("orders"))
                .thenThrow(new DatabaseException("Failed to get series details"))
                .thenReturn(new SeriesDetail("idfs_orders", "ORD"));

        assertThatExceptionOfType(DatabaseException.class)
                .isThrownBy(() -> seriesToSequenceMapper.getSeriesDetails("orders"))
                .withMessage("Failed to get series details");
        assertThat(seriesToSequenceMapper.getSeriesDetails("orders")).contains(new SeriesInfo("idfs_orders", "ORD"));
        verify(uniqueIdMapperRepository, times(2)).getSeriesDetails("orders");
    }
}
