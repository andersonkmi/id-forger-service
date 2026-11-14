package org.codecraftlabs.idgenerator.controller.idgeneration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.codecraftlabs.idgenerator.id.service.IdGenerationService;
import org.codecraftlabs.idgenerator.id.service.IdNotGeneratedException;
import org.codecraftlabs.idgenerator.id.service.InvalidFormatException;
import org.codecraftlabs.idgenerator.id.service.InvalidSeriesException;
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
 * Verifies that client errors on the hot path are logged as a single WARN line without a
 * stack trace, while server-side failures keep the ERROR level and the stack trace.
 */
@ExtendWith(MockitoExtension.class)
public class IdGenerationControllerLoggingTest {
    @Mock
    private IdGenerationService idGenerationService;

    private IdGenerationController controller;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    public void setUp() {
        controller = new IdGenerationController(idGenerationService);
        logger = (Logger) LoggerFactory.getLogger(IdGenerationController.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    public void should_log_unknown_series_as_warn_without_stack_trace() {
        when(idGenerationService.generateId("unknown", null))
                .thenThrow(new InvalidSeriesException("Invalid series name provided"));

        assertThatThrownBy(() -> controller.getNextId("unknown", null))
                .isInstanceOf(ResponseStatusException.class);

        assertSingleEvent(Level.WARN, false);
    }

    @Test
    public void should_log_invalid_format_as_warn_without_stack_trace() {
        when(idGenerationService.generateId("default", "bogus"))
                .thenThrow(new InvalidFormatException("Invalid format requested"));

        assertThatThrownBy(() -> controller.getNextId("default", "bogus"))
                .isInstanceOf(ResponseStatusException.class);

        assertSingleEvent(Level.WARN, false);
    }

    @Test
    public void should_log_generation_failure_as_error_with_stack_trace() {
        when(idGenerationService.generateId("default", null))
                .thenThrow(new IdNotGeneratedException("Failed to generate id", new RuntimeException("db down")));

        assertThatThrownBy(() -> controller.getNextId("default", null))
                .isInstanceOf(ResponseStatusException.class);

        assertSingleEvent(Level.ERROR, true);
    }

    private void assertSingleEvent(Level level, boolean withStackTrace) {
        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(level);
        if (withStackTrace) {
            assertThat(event.getThrowableProxy()).isNotNull();
        } else {
            assertThat(event.getThrowableProxy()).isNull();
        }
    }
}
