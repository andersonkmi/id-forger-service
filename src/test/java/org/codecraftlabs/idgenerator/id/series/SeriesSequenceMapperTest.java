package org.codecraftlabs.idgenerator.id.series;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SeriesSequenceMapperTest {
    private SeriesSequenceMapper seriesSequenceMapper;

    @BeforeEach
    public void setup() {
        this.seriesSequenceMapper = new SeriesSequenceMapper();
    }

    @Test
    public void all_enum_entries_should_be_loaded_into_mapper() {
        for (SeriesToSequence item : SeriesToSequence.values()) {
            assertThat(seriesSequenceMapper.getSequenceBySeriesName(item.getSeriesName())).isPresent();
        }
    }

    @Test
    public void default_series_should_map_to_default_sequence() {
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("default"))
                .hasValue("default_sequence");
    }

    @Test
    public void product_series_should_map_to_product_sequence() {
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("product"))
                .hasValue("product_sequence");
    }

    @Test
    public void unknown_series_name_should_return_empty() {
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("fake")).isEmpty();
    }

    @Test
    public void empty_series_name_should_return_empty() {
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("")).isEmpty();
    }

    @Test
    public void series_name_lookup_should_be_case_sensitive() {
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("Default")).isEmpty();
        assertThat(seriesSequenceMapper.getSequenceBySeriesName("DEFAULT")).isEmpty();
    }
}
