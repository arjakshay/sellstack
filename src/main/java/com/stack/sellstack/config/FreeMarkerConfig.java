package com.stack.sellstack.config;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.io.IOException;

@org.springframework.context.annotation.Configuration
public class FreeMarkerConfig {

    @Bean
    @Primary
    public Configuration freemarkerConfiguration() throws IOException {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        // Set the directory where template files are stored
        cfg.setClassForTemplateLoading(this.getClass(), "/templates/");

        // Recommended settings for Spring Boot
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);

        // Auto-import settings
        cfg.setAutoImports(new java.util.HashMap<>() {{
            put("spring", "spring.ftl");
        }});

        return cfg;
    }
}