package com.suspega.controller;

import com.suspega.model.Role;
import com.suspega.model.User;
import com.suspega.repository.RoleRepository;
import com.suspega.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
            .map(user -> new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().iterator().next().getName(),
                user.getSubscription().name(),
                user.isBlocked()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow();
        Role newRole = roleRepository.findByName(request.role()).orElseThrow();
        
        user.getRoles().clear();
        user.getRoles().add(newRole);
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/users/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setBlocked(!user.isBlocked());
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/users/{id}/subscription")
    public ResponseEntity<?> updateSubscription(@PathVariable Long id, @RequestBody SubscriptionUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow();
        user.setSubscription(com.suspega.model.Subscription.valueOf(request.subscription().toUpperCase()));
        userRepository.save(user);
        
        return ResponseEntity.ok().build();
    }
    
    record UserDto(Long id, String username, String email, String role, String subscription, boolean blocked) {}
    record RoleUpdateRequest(String role) {}
    record SubscriptionUpdateRequest(String subscription) {}
}