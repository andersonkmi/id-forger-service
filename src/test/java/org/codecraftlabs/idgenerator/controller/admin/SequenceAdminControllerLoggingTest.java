package org.codecraftlabs.idgenerator.controller.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.codecraftlabs.idgenerator.id.service.SequenceAdminService;
import org.codecraftlabs.idgenerator.id.service.SeriesAlreadyExistsException;
import org.codecraftlabs.idgenerator.id.service.SeriesData;
import org.codecraftlabs.idgenerator.id.service.SeriesNotCreatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Verifies that an already-existing series (a client conflict) is logged as a single WARN
 * line without a stack trace, while genuine creation failures keep ERROR and the stack trace.
 */
@ExtendWith(MockitoExtension.class)
public class SequenceAdminControllerLoggingTest {
    private static final SeriesData REQUEST = new SeriesData("orders", "ORD", null, null, null, null);

    @Mock
    private SequenceAdminService sequenceAdminService;

    private SequenceAdminController controller;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setUp() {
        controller = new SequenceAdminController(sequenceAdminService);
        logger = (Logger) LoggerFactory.getLogger(SequenceAdminController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    public void should_log_existing_series_as_warn_without_stack_trace() {
        when(sequenceAdminService.createSeries(REQUEST))
                .thenThrow(new SeriesAlreadyExistsException("Series already exists", new RuntimeException()));

        assertThatThrownBy(() -> controller.createSeries(request()))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
    }

    @Test
    public void should_log_creation_failure_as_error_with_stack_trace() {
        when(sequenceAdminService.createSeries(REQUEST))
                .thenThrow(new SeriesNotCreatedException("Failed to create sequence", new RuntimeException("db down")));

        assertThatThrownBy(() -> controller.createSeries(request()))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.ERROR);
        assertThat(appender.list.getFirst().getThrowableProxy()).isNotNull();
    }

    private static Series request() {
        Series series = new Series();
        series.setName("orders");
        series.setPrefix("ORD");
        return series;
    }
}
