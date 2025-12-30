package com.buyone.userservice.service;

import com.buyone.userservice.exception.BadRequestException;
import com.buyone.userservice.exception.ConflictException;
import com.buyone.userservice.exception.ResourceNotFoundException;
import com.buyone.userservice.model.Role;
import com.buyone.userservice.model.User;
import com.buyone.userservice.repository.UserRepository;
import com.buyone.userservice.request.RegisterUserRequest;
import com.buyone.userservice.request.UpdateUserRequest;
import com.buyone.userservice.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    // -------- createUser --------
    
    @Test
    void createUser_savesAndReturnsUser_whenRequestIsValid() {
        // Given
        RegisterUserRequest req = new RegisterUserRequest();
        req.setName("Alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret");
        req.setRole(Role.CLIENT);
        req.setAvatar("avatar.png");
        
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("ENC(secret)");
        
        User saved = User.builder()
                .id("u1")
                .name("Alice")
                .email("alice@example.com")
                .password("ENC(secret)")
                .role(Role.CLIENT)
                .avatar("avatar.png")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        
        // When
        UserResponse response = userService.createUser(req);
        
        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User toSave = userCaptor.getValue();
        
        assertThat(toSave.getEmail()).isEqualTo("alice@example.com");
        assertThat(toSave.getPassword()).isEqualTo("ENC(secret)");
        assertThat(response.getId()).isEqualTo("u1");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CLIENT);
    }
    
    @Test
    void createUser_throwsBadRequest_whenEmailEmpty() {
        RegisterUserRequest req = new RegisterUserRequest();
        req.setEmail("  ");
        req.setPassword("secret");
        
        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email cannot be empty");
    }
    
    @Test
    void createUser_throwsConflict_whenEmailExists() {
        RegisterUserRequest req = new RegisterUserRequest();
        req.setEmail("alice@example.com");
        req.setPassword("secret");
        
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(new User()));
        
        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }
    
    @Test
    void createUser_throwsBadRequest_whenPasswordEmpty() {
        RegisterUserRequest req = new RegisterUserRequest();
        req.setEmail("alice@example.com");
        req.setPassword("   ");
        
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password cannot be empty");
    }
    
    // -------- getUserById / getUserByEmail --------
    
    @Test
    void getUserById_returnsUserResponse_whenFound() {
        User user = User.builder()
                .id("u1")
                .name("Bob")
                .email("bob@example.com")
                .role(Role.CLIENT)
                .build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        
        UserResponse response = userService.getUserById("u1");
        
        assertThat(response.getId()).isEqualTo("u1");
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
    }
    
    @Test
    void getUserById_throwsNotFound_whenMissing() {
        when(userRepository.findById("u1")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.getUserById("u1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID");
    }
    
    @Test
    void getUserByEmail_throwsBadRequest_whenEmailEmpty() {
        assertThatThrownBy(() -> userService.getUserByEmail("  "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be empty");
    }
    
    @Test
    void getUserByEmail_throwsNotFound_whenMissing() {
        when(userRepository.findByEmail("x@example.com")).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.getUserByEmail("x@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No user found with email");
    }
    
    // -------- getAllUsers / getUsersByRole --------
    
    @Test
    void getAllUsers_returnsList_whenUsersExist() {
        User u1 = User.builder().id("1").email("a@example.com").role(Role.CLIENT).build();
        User u2 = User.builder().id("2").email("b@example.com").role(Role.SELLER).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        
        List<UserResponse> responses = userService.getAllUsers();
        
        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(UserResponse::getId).containsExactlyInAnyOrder("1", "2");
    }
    
    @Test
    void getAllUsers_throwsNotFound_whenEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());
        
        assertThatThrownBy(userService::getAllUsers)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No users found");
    }
    
    @Test
    void getUsersByRole_returnsMappedResponses() {
        User u1 = User.builder().id("1").email("a@example.com").role(Role.CLIENT).build();
        when(userRepository.findByRole(Role.CLIENT)).thenReturn(List.of(u1));
        
        List<UserResponse> responses = userService.getUsersByRole(Role.CLIENT);
        
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo("1");
    }
    
    // -------- updateUser --------
    
    @Test
    void updateUser_updatesFieldsAndSaves() {
        User existing = User.builder()
                .id("u1")
                .name("Old")
                .email("old@example.com")
                .password("OLDPWD")
                .role(Role.CLIENT)
                .avatar("old.png")
                .build();
        
        UpdateUserRequest req = new UpdateUserRequest();
        req.setName("New");
        req.setEmail("new@example.com");
        req.setPassword("newpwd");
        req.setRole(Role.SELLER);
        req.setAvatar("new.png");
        
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newpwd")).thenReturn("ENC(newpwd)");
        
        User saved = User.builder()
                .id(existing.getId())
                .name("New")
                .email("new@example.com")
                .password("ENC(newpwd)")
                .role(Role.SELLER)
                .avatar("new.png")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        
        UserResponse response = userService.updateUser("u1", req);
        
        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        assertThat(response.getRole()).isEqualTo(Role.SELLER);
        assertThat(response.getAvatar()).isEqualTo("new.png");
    }
    
    @Test
    void updateUser_throwsConflict_whenEmailTakenByOtherUser() {
        User existing = User.builder()
                .id("u1")
                .email("old@example.com")
                .build();
        
        UpdateUserRequest req = new UpdateUserRequest();
        req.setEmail("taken@example.com");
        
        when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(new User())); // some other user
        
        assertThatThrownBy(() -> userService.updateUser("u1", req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already exists");
    }
    
    // -------- updateUserByEmail --------
    
    @Test
    void updateUserByEmail_throwsBadRequest_whenEmailEmpty() {
        UpdateUserRequest req = new UpdateUserRequest();
        assertThatThrownBy(() -> userService.updateUserByEmail("  ", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email must not be empty");
    }
    
    @Test
    void updateUserByEmail_updatesNamePasswordAvatar() {
        User existing = User.builder()
                .id("u1")
                .name("Old")
                .email("user@example.com")
                .password("OLDPWD")
                .avatar("old.png")
                .role(Role.CLIENT)
                .build();
        
        UpdateUserRequest req = new UpdateUserRequest();
        req.setName("New");
        req.setPassword("newpwd");
        req.setAvatar("new.png");
        
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newpwd")).thenReturn("ENC(newpwd)");
        
        User saved = User.builder()
                .id(existing.getId())
                .name("New")
                .email(existing.getEmail())
                .password("ENC(newpwd)")
                .avatar("new.png")
                .role(existing.getRole())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        
        UserResponse response = userService.updateUserByEmail("user@example.com", req);
        
        assertThat(response.getName()).isEqualTo("New");
        assertThat(response.getAvatar()).isEqualTo("new.png");
    }
    
    // -------- deleteUser --------
    
    @Test
    void deleteUser_callsRepository_whenExists() {
        when(userRepository.existsById("u1")).thenReturn(true);
        
        userService.deleteUser("u1");
        
        verify(userRepository).deleteById("u1");
    }
    
    @Test
    void deleteUser_throwsNotFound_whenMissing() {
        when(userRepository.existsById("u1")).thenReturn(false);
        
        assertThatThrownBy(() -> userService.deleteUser("u1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cannot delete — user not found");
    }
}
