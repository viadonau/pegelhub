package at.pegelhub.shared.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for the WebMvc Layer.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    LocaleResolver localeResolver() {
        return new OpenApiLocaleResolver();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToUUIDConverter());
    }
}
