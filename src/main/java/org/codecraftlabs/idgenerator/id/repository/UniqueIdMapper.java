package org.codecraftlabs.idgenerator.id.repository;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.codecraftlabs.idgenerator.id.Sequence;

/**
 * MyBatis mapper interface for unique ID generation operations.
 * Provides direct database access to PostgreSQL sequences for retrieving,
 * advancing, and updating sequence values.
 */
@Mapper
public interface UniqueIdMapper {
    /**
     * Retrieves the next value from the specified database sequence.
     *
     * @param seriesName the name of the database sequence
     * @return the next value in the sequence
     */
    @Select("SELECT NEXTVAL(#{seriesName})")
    long getNextId(@Nonnull @Param("seriesName") String seriesName);


    /**
     * Retrieves the current value of the specified database sequence
     * without advancing it.
     *
     * @param sequenceName the name of the database sequence
     * @return the current value of the sequence
     */
    @Select("SELECT last_value FROM pg_sequences WHERE sequencename = #{sequenceName}")
    Long getCurrentSequenceValue(@Nonnull @Param("sequenceName") String sequenceName);

    /**
     * Retrieves the details of a database sequence, including its metadata.
     *
     * @param schema the database schema containing the sequence
     * @param name   the name of the sequence
     * @return a {@link Sequence} object containing the sequence details
     */
    @Select("""
            SELECT schemaname schema, sequencename name, start_value, min_value, max_value, increment_by, cycle, cache_size, last_value
            FROM pg_sequences
            WHERE schemaname = #{schema} AND sequencename = #{name}
            """)
    @ConstructorArgs({
            @Arg(column = "schema", javaType = String.class),
            @Arg(column = "name", javaType = String.class),
            @Arg(column = "start_value", javaType = Long.class),
            @Arg(column = "min_value", javaType = Long.class),
            @Arg(column = "max_value", javaType = Long.class),
            @Arg(column = "increment_by", javaType = Long.class),
            @Arg(column = "cache_size", javaType = Long.class),
            @Arg(column = "cycle", javaType = Boolean.class),
            @Arg(column = "last_value", javaType = Long.class)
    })
    Sequence getSequenceDetails(@Nonnull @Param("schema") String schema, @Nonnull @Param("name") String name);

    @Select("""
            SELECT sequence, prefix 
            FROM series_details
            WHERE name = #{name};
            """)
    @ConstructorArgs({
            @Arg(column = "sequence", javaType = String.class),
            @Arg(column = "prefix", javaType = String.class)
    })
    SeriesDetail getSeriesDetails(@Nonnull @Param("name") String name);


    /**
     * Updates the last value of the specified database sequence.
     *
     * @param name      the name of the sequence to update
     * @param lastValue the new last value to set on the sequence
     */
    @Update("SELECT setval(#{name}, #{lastValue})")
    void updateSequenceLastValue(@Nonnull @Param("name") String name, @Param("lastValue") long lastValue);

    /**
     * Creates a new database sequence with the given configuration.
     * <p>
     * PostgreSQL DDL statements do not support bind parameters, so the schema and
     * name are substituted directly into the statement. Callers must ensure these
     * values are validated identifiers and never raw user input.
     *
     * @param schema      the database schema that will own the sequence
     * @param name        the name of the sequence to create
     * @param startValue  the value the sequence starts at
     * @param incrementBy the step size between consecutive values
     * @param minValue    the minimum allowed value
     * @param maxValue    the maximum allowed value
     */
    @Update("""
            <script>
            CREATE SEQUENCE ${schema}.${name}
            <if test="startValue != null">START WITH ${startValue}</if>
            <if test="incrementBy != null">INCREMENT BY ${incrementBy}</if>
            <if test="minValue != null">MINVALUE ${minValue}</if>
            <if test="maxValue != null">MAXVALUE ${maxValue}</if>
            </script>
            """)
    void createSeries(@Nonnull @Param("schema") String schema,
                      @Nonnull @Param("name") String name,
                      @Nullable @Param("startValue") Long startValue,
                      @Nullable @Param("incrementBy") Long incrementBy,
                      @Nullable @Param("minValue") Long minValue,
                      @Nullable @Param("maxValue") Long maxValue);

    /**
     * Registers a series in the {@code series_details} table, mapping it to its
     * backing sequence and identifier prefix.
     *
     * @param name         the series name
     * @param sequenceName the name of the backing database sequence
     * @param prefix       the prefix applied to identifiers generated for the series
     */
    @Insert("INSERT INTO series_details (name, sequence, prefix) VALUES (#{name}, #{sequenceName}, #{prefix})")
    void insertSeriesDetails(@Nonnull @Param("name") String name,
                             @Nonnull @Param("sequenceName") String sequenceName,
                             @Nonnull @Param("prefix") String prefix);
}
