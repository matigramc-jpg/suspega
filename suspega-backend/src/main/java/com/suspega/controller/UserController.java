package com.suspega.controller;

import com.suspega.model.User;
import com.suspega.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        
        return ResponseEntity.ok(new UserResponse(
            user.getUsername(),
            user.getEmail(),
            user.getRoles().iterator().next().getName(),
            user.getSubscription().name(),
            user.isBlocked()
        ));
    }
    
    record UserResponse(String username, String email, String role, String subscription, boolean blocked) {}
}