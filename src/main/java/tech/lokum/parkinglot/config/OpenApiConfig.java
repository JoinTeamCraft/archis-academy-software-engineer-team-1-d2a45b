package tech.lokum.parkinglot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 documentation configuration for interactive Swagger UI and API specifications.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parking Lot Management System API")
                        .description("""
                                ## Overview
                                Comprehensive RESTful API for the **Parking Lot Management System**, providing automated 
                                parking lot discovery, spot reservation lifecycle management, financial transaction settlement 
                                with external gateways (e.g., Stripe), analytical reporting, and role-based access control.
                                
                                ### Authentication & Authorization
                                Protected endpoints require a JSON Web Token (JWT) provided in the HTTP `Authorization` header:
                                ```
                                Authorization: Bearer <your_jwt_token>
                                ```
                                Authenticate via `POST /api/auth/login` or `POST /api/auth/register` to receive a token.
                                Click the **Authorize** button on the right to test protected endpoints interactively.
                                
                                ### Roles & Permissions (RBAC)
                                - **ADMIN**: Full system administration including user creation and deletion.
                                - **MANAGER**: User viewing, reservation management, and report generation.
                                - **OPERATOR**: Lot operations, spot updates, and operational report access.
                                - **CUSTOMER**: Personal vehicle registration, spot booking, and payment processing.
                                
                                ### Standard Response Codes
                                - `200 OK`: Request succeeded.
                                - `201 Created`: Resource successfully created.
                                - `204 No Content`: Resource deleted or deactivated.
                                - `400 Bad Request`: Input validation failed or invalid payload syntax.
                                - `401 Unauthorized`: Missing or invalid authentication token.
                                - `403 Forbidden`: Insufficient role permissions for the resource.
                                - `404 Not Found`: Target resource could not be found.
                                - `409 Conflict`: Business constraint violation (e.g. double booking, duplicate entity).
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Lokum Engineering Team")
                                .email("engineering@parkinglot.lokum.tech")
                                .url("https://parkinglot.lokum.tech"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.parkinglot.lokum.tech")
                                .description("Production Server")
                ))
                .tags(List.of(
                        new Tag().name("Authentication").description("Registration, login, and JWT token issuance"),
                        new Tag().name("Users").description("User management endpoints protected by RBAC"),
                        new Tag().name("Parking Lots").description("Parking facilities and capacity management"),
                        new Tag().name("Reservations").description("Spot booking, rescheduling, and reservation lifecycles"),
                        new Tag().name("Payments").description("Payment initiation, verification, and Stripe webhooks"),
                        new Tag().name("Reports").description("Analytical reports (User Activity, Booking Trends, Payment Summaries)"),
                        new Tag().name("Feedback").description("User feedback about the application or service"),
                        new Tag().name("System Status").description("System heartbeat and service health monitoring")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token in the format: `Bearer <token>`")));
    }
}
