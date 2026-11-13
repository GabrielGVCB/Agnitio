package com.agnitio.infrastructure;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Minimal Spring Boot configuration for slice tests (e.g. {@code @DataJpaTest}) in the
 * infrastructure module, which has no {@code @SpringBootApplication} of its own.
 * Entity and repository scanning start from this package.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class InfrastructureTestConfiguration {
}
