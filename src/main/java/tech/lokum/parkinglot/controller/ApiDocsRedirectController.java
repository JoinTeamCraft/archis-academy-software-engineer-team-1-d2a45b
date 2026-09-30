package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller providing convenience redirects to Swagger UI and OpenAPI documentation endpoints.
 */
@Controller
@Hidden
public class ApiDocsRedirectController {

    @GetMapping({"/docs", "/documentation", "/swagger"})
    public String redirectToSwaggerUi() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/v3/api-docs")
    public String forwardToApiDocs() {
        return "forward:/api-docs";
    }
}
