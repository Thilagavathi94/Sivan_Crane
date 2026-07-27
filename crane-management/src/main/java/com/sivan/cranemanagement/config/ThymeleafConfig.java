package com.sivan.cranemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

/**
 * Registers the Thymeleaf Spring Security dialect.
 *
 * fragments.html uses xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
 * and the sec:authentication="name" attribute. Having the
 * thymeleaf-extras-springsecurity6 JAR on the classpath (declared in pom.xml)
 * is NOT enough on its own — Spring Boot does not auto-register this dialect,
 * it must be exposed as a bean. Without this bean, Thymeleaf does not
 * recognize the "sec" attribute and fails while parsing ANY template that
 * includes the topbar/sidebar fragment (i.e. every page: drivers, bookings,
 * tripsheets, quotations, payments, expenses, cranes, dashboard, customers,
 * invoices, reports) with:
 *
 *   org.thymeleaf.exceptions.TemplateInputException: An error happened
 *   during template parsing (template: "class path resource [templates/...]")
 *
 * Adding this bean fixes all of those pages at once.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}