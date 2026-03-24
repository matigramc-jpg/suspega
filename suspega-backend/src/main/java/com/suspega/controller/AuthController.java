package com.suspega.controller;

import com.suspega.dto.AuthResponse;
import com.suspega.dto.LoginRequest;
import com.suspega.dto.RegisterRequest;
import com.suspega.model.Role;
import com.suspega.model.User;
import com.suspega.repository.RoleRepository;
import com.suspega.repository.UserRepository;
import com.suspega.config.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private static final String INVITE_CODE = "PEGASUS123";
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtService jwtService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtService.generateToken(authentication);
        
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String role = user.getRoles().iterator().next().getName();
        
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail(), role, user.getSubscription().name()));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Sprawdź kod zaproszenia
        if (!INVITE_CODE.equals(request.getInviteCode())) {
            return ResponseEntity.badRequest().body("Invalid invite code");
        }
        
        // Sprawdź czy użytkownik już istnieje
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        
        // Sprawdź czy to pierwszy użytkownik
        boolean isFirstUser = userRepository.count() == 0;
        
        // Utwórz nowego użytkownika
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Przypisz role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> roleRepository.save(new Role("USER")));
        roles.add(userRole);
        
        if (isFirstUser) {
            Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
            roles.add(adminRole);
        }
        
        user.setRoles(roles);
        userRepository.save(user);
        
        // Zaloguj nowego użytkownika
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        String token = jwtService.generateToken(authentication);
        
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername(), user.getEmail(), 
            isFirstUser ? "ADMIN" : "USER", user.getSubscription().name()));
    }
}