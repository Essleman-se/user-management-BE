package micronet.user.controller;

import micronet.user.dto.UserPatchDTO;
import micronet.user.dto.UserRequestDTO;
import micronet.user.dto.UserResponseDTO;
import micronet.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello, World!!!!!!!!!!!!!");
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users - Get all users
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/users/email/{email} - Get user by email
     * Note: This must come before /{id} to avoid path matching conflicts
     * Note: Email must be URL encoded (e.g., @ becomes %40)
     * Alternative: Use /api/users/by-email?email=... for easier testing
     */
    @GetMapping("/email/{email:.+}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(
            @PathVariable @Email(message = "Invalid email format") String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/users/by-email?email={email} - Get user by email (query parameter)
     * Alternative endpoint that doesn't require URL encoding
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponseDTO> getUserByEmailQuery(
            @RequestParam @Email(message = "Invalid email format") String email) {
        UserResponseDTO user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * GET /api/users/{id} - Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable @Min(value = 1, message = "User ID must be greater than 0") Long id) {
        UserResponseDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/users - Create a new user
     */
    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * PUT /api/users/{id} - Update an existing user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable @Min(value = 1, message = "User ID must be greater than 0") Long id,
            @Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserResponseDTO updated = userService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/users/{id} - Partially update a user
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponseDTO> patchUser(
            @PathVariable @Min(value = 1, message = "User ID must be greater than 0") Long id,
            @Valid @RequestBody UserPatchDTO userPatchDTO) {
        UserResponseDTO patched = userService.patchUser(id, userPatchDTO);
        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE /api/users/{id} - Delete a user (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Min(value = 1, message = "User ID must be greater than 0") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
