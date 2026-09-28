package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.dto.PageResponse;
import tech.lokum.parkinglot.dto.UserResponse;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.security.JwtService;
import tech.lokum.parkinglot.service.UserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    private String createToken(String email, Role role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }

    @Nested
    @DisplayName("DELETE /api/users/{id} - Role Restrictions")
    class DeleteUserSecurityTests {

        @Test
        @DisplayName("DELETE /api/users/{id} should succeed with 204 No Content when called by ADMIN")
        void adminCanDeleteUser() throws Exception {
            doNothing().when(userService).deleteUser(1L);

            mockMvc.perform(delete("/api/users/1")
                            .header("Authorization", createToken("admin@test.com", Role.ADMIN)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/users/{id} should return 403 Forbidden when called by MANAGER")
        void managerCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/1")
                            .header("Authorization", createToken("manager@test.com", Role.MANAGER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("DELETE /api/users/{id} should return 403 Forbidden when called by USER")
        void userCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/1")
                            .header("Authorization", createToken("user@test.com", Role.USER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("DELETE /api/users/{id} should return 401 Unauthorized when unauthenticated")
        void unauthenticatedCannotDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} - Role Restrictions")
    class GetUserByIdSecurityTests {

        @Test
        @DisplayName("GET /api/users/{id} should succeed with 200 OK when called by ADMIN")
        void adminCanGetUserById() throws Exception {
            UserResponse response = new UserResponse(1L, "admin@test.com", "admin", Role.ADMIN);
            when(userService.getUserById(eq(1L))).thenReturn(response);

            mockMvc.perform(get("/api/users/1")
                            .header("Authorization", createToken("admin@test.com", Role.ADMIN))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("admin@test.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("GET /api/users/{id} should succeed with 200 OK when called by MANAGER")
        void managerCanGetUserById() throws Exception {
            UserResponse response = new UserResponse(2L, "manager@test.com", "manager", Role.MANAGER);
            when(userService.getUserById(eq(2L))).thenReturn(response);

            mockMvc.perform(get("/api/users/2")
                            .header("Authorization", createToken("manager@test.com", Role.MANAGER))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.email").value("manager@test.com"))
                    .andExpect(jsonPath("$.role").value("MANAGER"));
        }

        @Test
        @DisplayName("GET /api/users/{id} should return 403 Forbidden when called by regular USER")
        void userCannotGetUserById() throws Exception {
            mockMvc.perform(get("/api/users/1")
                            .header("Authorization", createToken("user@test.com", Role.USER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("GET /api/users/{id} should return 401 Unauthorized when unauthenticated")
        void unauthenticatedCannotGetUserById() throws Exception {
            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }
    }

    @Nested
    @DisplayName("GET /api/users - Role Restrictions")
    class GetAllUsersSecurityTests {

        @Test
        @DisplayName("GET /api/users should return 200 OK and paginated users when called by ADMIN")
        void adminCanListUsers() throws Exception {
            UserResponse u1 = new UserResponse(1L, "admin@test.com", "admin", Role.ADMIN);
            PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(u1), 0, 20, 1, 1, true, true);
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/users")
                            .header("Authorization", createToken("admin@test.com", Role.ADMIN))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("admin@test.com"));
        }

        @Test
        @DisplayName("GET /api/users should return 200 OK when called by MANAGER")
        void managerCanListUsers() throws Exception {
            UserResponse u1 = new UserResponse(2L, "manager@test.com", "manager", Role.MANAGER);
            PageResponse<UserResponse> pageResponse = new PageResponse<>(List.of(u1), 0, 20, 1, 1, true, true);
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(pageResponse);

            mockMvc.perform(get("/api/users")
                            .header("Authorization", createToken("manager@test.com", Role.MANAGER))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("manager@test.com"));
        }

        @Test
        @DisplayName("GET /api/users should return 403 Forbidden when called by regular USER")
        void userCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users")
                            .header("Authorization", createToken("user@test.com", Role.USER)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("GET /api/users should return 401 Unauthorized when unauthenticated")
        void unauthenticatedCannotListUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }
    }
}
