package com.utp.anarkiagames.config;

import com.culqi.Culqi;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CulquiConfig {
    @Value("${culqui.secretKey}")
    private String secretKey;

    @PostConstruct
    public void init(){
        Culqi.secret_key = secretKey;
    }
}
