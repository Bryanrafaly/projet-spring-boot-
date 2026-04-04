package com.example.demo.service;

import com.example.demo.dto.UserDtos.UserRequest;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String firstName, String lastName, String email, String password, String roleValue) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists");
        }

        Role role = roleValue == null || roleValue.isBlank()
                ? Role.AGENT_REGISTRATION
                : Role.valueOf(roleValue.trim().toUpperCase());

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();

        return userRepository.save(user);
    }

    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }

    public User updateUser(Long id, UserRequest request) {
        User user = findById(id);
        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A user with this email already exists");
                });
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(Role.valueOf(request.role().trim().toUpperCase()));
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.delete(findById(id));
    }

    public void updatePasswordByEmail(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
