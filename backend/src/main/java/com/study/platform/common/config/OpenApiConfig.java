package com.study.platform.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 文档，并声明 Bearer JWT 认证方案，供 Swagger UI 的 Authorize 使用。
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "Bearer";

    @Bean
    public OpenAPI smartStudyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Study Platform API")
                        .description("综合学习辅助平台接口文档")
                        .version("v1.0"))
                .addServersItem(new Server().url("http://localhost:8080").description("本地开发"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("请求头格式：Authorization: Bearer {accessToken}")));
    }
}
