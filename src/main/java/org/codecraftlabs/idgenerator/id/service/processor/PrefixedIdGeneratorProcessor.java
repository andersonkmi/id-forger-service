package org.codecraftlabs.idgenerator.id.service.processor;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.series.SeriesToSequenceMapper;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

/**
 * {@link IdFormatProcessor} that prepends the series prefix and zero-pads the ID to
 * 15 digits, producing identifiers such as {@code STD000000000012345}.
 *
 * <p>The prefix is resolved per series through {@link SeriesToSequenceMapper}, which
 * serves it from an in-memory cache backed by the {@code series_details} table. The
 * numeric part is obtained from {@link IdGenerator}, which advances the underlying
 * database sequence. Values wider than 15 digits are not truncated, so the numeric
 * part is <em>at least</em> 15 digits long.</p>
 *
 * <p>Registered as the {@code prefixed} format processor. Instances are stateless
 * apart from their collaborators and are safe for concurrent use.</p>
 */
@Service("prefixed")
class PrefixedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;
    private final SeriesToSequenceMapper seriesToSequenceMapper;

    /**
     * Creates the processor with its collaborators.
     *
     * @param idGenerator            generator used to advance the series sequence and
     *                               obtain the raw numeric ID
     * @param seriesToSequenceMapper mapper used to resolve the prefix configured for
     *                               a series
     */
    @Autowired
    PrefixedIdGeneratorProcessor(@Nonnull IdGenerator idGenerator, @Nonnull SeriesToSequenceMapper seriesToSequenceMapper) {
        this.idGenerator = idGenerator;
        this.seriesToSequenceMapper = seriesToSequenceMapper;
    }

    /**
     * Generates the next ID for the given series and formats it as the series prefix
     * followed by the sequence value left-padded with zeros to 15 digits.
     *
     * <p>The series is resolved before the sequence is advanced, so an unknown series
     * never consumes a sequence value.</p>
     *
     * @param seriesName the series name
     * @return the prefixed, zero-padded ID (e.g. {@code STD000000000012345})
     * @throws InvalidSeriesException if the series has no mapping in
     *         {@code series_details}, or its sequence cannot be found
     * @throws org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException
     *         if advancing the sequence fails due to a database error
     * @throws org.codecraftlabs.idgenerator.id.repository.DatabaseException
     *         if loading the series details from the database fails
     */
    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        var seriesInfo = seriesToSequenceMapper.getSeriesDetails(seriesName);
        if (seriesInfo.isEmpty()) {
            throw new InvalidSeriesException("Series not mapped yet");
        }
        long id = idGenerator.generateLongId(seriesName);
        String prefix = seriesInfo.get().prefix();
        return format("%s%015d", prefix, id);
    }
}
