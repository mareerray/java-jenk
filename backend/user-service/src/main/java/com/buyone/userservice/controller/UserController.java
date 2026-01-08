package com.buyone.userservice.controller;

import com.buyone.userservice.model.Role;
import com.buyone.userservice.request.UpdateUserRequest;
import com.buyone.userservice.response.UserResponse;
import com.buyone.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    // ---------------------- //
    
    // GET /api/users/{id} - Find user by ID (admin/internal)
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    // GET /api/users - List all users (admin/internal)
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    // GET /me - current user profile
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        String email = principal.getName();
        UserResponse user = userService.getUserByEmail(email);
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/sellers")
    public ResponseEntity<List<UserResponse>> getSellers() {
        return ResponseEntity.ok(userService.getUsersByRole(Role.SELLER));
    }
    
    @GetMapping("/clients")
    public ResponseEntity<List<UserResponse>> getClients() {
        return ResponseEntity.ok(userService.getUsersByRole(Role.CLIENT));
    }
    
    // ---------------------- //
    
    // PUT /me - update current user profile
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Principal principal,
            @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        String email = principal.getName();
        UserResponse updated = userService.updateUserByEmail(email, updateUserRequest);
        return ResponseEntity.ok()
            .header("X-Email-Update", "Email changes require admin privileges")
            .body(updated);
    }

    // PUT /api/users/{id} - Update user info (admin/internal)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        UserResponse updated = userService.updateUser(id, updateUserRequest);
        return ResponseEntity.ok(updated);
    }
    
    // DELETE /api/users/{id} - Delete user by ID (admin/internal)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}



