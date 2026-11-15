package org.codecraftlabs.idgenerator.api.repository;

import jakarta.annotation.Nonnull;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * MyBatis mapper interface for unique ID generation operations.
 * Only advances existing PostgreSQL sequences; creating, resetting and dropping
 * sequences is the responsibility of the admin service.
 */
@Mapper
public interface UniqueIdMapper {
    /**
     * Resolves a series through {@code series_details} and advances its backing sequence
     * in a single statement, returning the new value together with the series prefix.
     *
     * <p>The {@code WHERE} clause is applied before the select list is evaluated, so an
     * unknown series returns no row and never consumes a sequence value. If the series is
     * deleted concurrently, {@code nextval} fails with SQLState {@code 42P01}
     * (undefined table).</p>
     *
     * @param name the series name
     * @return the generated value and prefix, or {@code null} if the series does not exist
     */
    @Select("""
            SELECT nextval(sequence::regclass) AS next_value, prefix
            FROM series_details
            WHERE name = #{name}
            """)
    @ConstructorArgs({
            @Arg(column = "next_value", javaType = long.class),
            @Arg(column = "prefix", javaType = String.class)
    })
    GeneratedId getNextIdForSeries(@Nonnull @Param("name") String name);
}
