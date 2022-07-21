package com.lizaapp.CreditConveyor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Credit conveyor")
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .email("zefna@yandex.ru")
                                                .name("Elizaveta Grinchuk")
                                )
                );
    }
}
