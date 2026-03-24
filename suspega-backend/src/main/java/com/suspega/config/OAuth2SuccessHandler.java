package com.suspega.config;

import com.suspega.model.Role;
import com.suspega.model.User;
import com.suspega.repository.RoleRepository;
import com.suspega.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId(); // "google" lub "discord"
        
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String providerId = oauthUser.getAttribute("sub"); // dla Google, dla Discord inaczej
        
        // Discord ma inne nazwy atrybutów
        if ("discord".equals(provider)) {
            providerId = oauthUser.getAttribute("id");
            if (name == null) name = oauthUser.getAttribute("username");
        }
        
        // Sprawdź czy użytkownik już istnieje
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user == null) {
            // Nowy użytkownik – trzeba podać kod zaproszenia
            // Przekieruj na frontend z informacją, że potrzebny kod
            String redirectUrl = "https://suspega.onrender.com/oauth-callback?provider=" + provider + 
                                 "&email=" + email + "&name=" + name + "&providerId=" + providerId;
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }
        
        // Logowanie istniejącego użytkownika
        String token = generateTokenForUser(user);
        String redirectUrl = "https://suspega.onrender.com/oauth-success?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
    
    private String generateTokenForUser(User user) {
        // Tworzenie tymczasowego Authentication obiektu
        // Użyj JwtService do wygenerowania tokena
        return jwtService.generateTokenForUser(user.getUsername());
    }
}