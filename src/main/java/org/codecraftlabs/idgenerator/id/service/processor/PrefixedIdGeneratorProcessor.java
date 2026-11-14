package org.codecraftlabs.idgenerator.id.service.processor;

import jakarta.annotation.Nonnull;
import org.codecraftlabs.idgenerator.id.repository.GeneratedId;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

/**
 * {@link IdFormatProcessor} that prepends the series prefix and zero-pads the ID to
 * 15 digits, producing identifiers such as {@code STD000000000012345}.
 *
 * <p>The prefix is read from {@code series_details} in the same database round trip
 * that advances the sequence (see {@link IdGenerator#nextId(String)}), so it always
 * reflects the current series configuration. Values wider than 15 digits are not
 * truncated, so the numeric part is <em>at least</em> 15 digits long.</p>
 *
 * <p>Registered as the {@code prefixed} format processor. Instances are stateless
 * apart from their collaborator and are safe for concurrent use.</p>
 */
@Service("prefixed")
class PrefixedIdGeneratorProcessor implements IdFormatProcessor {
    private final IdGenerator idGenerator;

    /**
     * @param idGenerator generator used to advance the series sequence and obtain the prefix
     */
    PrefixedIdGeneratorProcessor(@Nonnull IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    /**
     * Generates the next ID for the given series and formats it as the series prefix
     * followed by the sequence value left-padded with zeros to 15 digits.
     *
     * @param seriesName the series name
     * @return the prefixed, zero-padded ID (e.g. {@code STD000000000012345})
     * @throws org.codecraftlabs.idgenerator.id.service.InvalidSeriesException
     *         if the series is unknown or its sequence no longer exists
     * @throws org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException
     *         if a database error occurs
     */
    @Nonnull
    @Override
    public String generateId(@Nonnull String seriesName) {
        GeneratedId generatedId = idGenerator.nextId(seriesName);
        return format("%s%015d", generatedId.prefix(), generatedId.value());
    }
}
