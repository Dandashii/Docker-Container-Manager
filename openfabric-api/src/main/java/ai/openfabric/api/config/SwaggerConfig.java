package ai.openfabric.api.config;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.RedirectView;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@Configuration
@EnableWebMvc
@EnableSwagger2
public class SwaggerConfig implements WebMvcConfigurer {
    @Value("${node.api.path}")
    private String REST_API_PREFIX;

    @Bean
    public Docket restApi() {

        List<SecurityScheme> securitySchemes = Lists.newArrayList(
                new ApiKey("Authorization", "Authorization", "header"));
        List<SecurityContext> securityContexts = Lists.newArrayList(
                xAuthTokenSecurityContext()
        );

        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.basePackage("ai.openfabric.api.controller"))
                .paths(PathSelectors.ant(REST_API_PREFIX + "/**"))
                .build()
                .groupName("REST-API")
                .apiInfo(getApiInfo())
                .securitySchemes(securitySchemes)
                .securityContexts(securityContexts);
    }

    private static ApiInfo getApiInfo() {
        return new ApiInfoBuilder()
                .title("Openfabric")
                .version("v1")
                .build();
    }

    private SecurityContext xAuthTokenSecurityContext() {
        return SecurityContext.builder()
                .securityReferences(Lists.newArrayList(
                        new SecurityReference("Authorization", new AuthorizationScope[0])))
                .build();
    }
}
