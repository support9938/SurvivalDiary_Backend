package com.survivaldiary.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(springdoc) 설정 — http://localhost:8080/swagger-ui.html
 * 팀원은 서버 코드를 읽지 않고 이 문서만으로 API 계약을 확인한다.
 * JWT 도입(#5) 후에는 우측 상단 Authorize 버튼에 액세스 토큰을 넣고 테스트한다.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("생존일기 (Survival Diary) API")
                        .description("청년 경제 자립 지원 앱 — 공통 응답 포맷: ApiResponse<T>, 페이징: PageResponse<T>")
                        .version("v0.1.0"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
