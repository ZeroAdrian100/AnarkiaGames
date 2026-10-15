package com.utp.anarkiagames.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita {@code @Async}: los correos se envían en otro hilo y no retrasan la respuesta. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
