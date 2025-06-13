package com.wowconsulting.controller;

import com.wowconsulting.model.User;
import com.wowconsulting.repository.UserRepository;
import com.wowconsulting.service.AuthService;
import com.wowconsulting.dto.AuthResponse;
import com.wowconsulting.dto.ApiResponse;
import com.wowconsulting.dto.UserDTO;

import com.wowconsulting.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Value("${discord.client-id}")
    private String discordClientId;
    @Value("${discord.client-secret}")
    private String discordClientSecret;
    @Value("${discord.redirect-uri}")
    private String discordRedirectUri;
    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/discord")
    public CompletableFuture<ResponseEntity<ApiResponse<AuthResponse>>> authenticateWithDiscord(
            @RequestBody Map<String, String> request) {

        String discordId = request.get("discordId");
        String username = request.get("username");
        String discriminator = request.get("discriminator");
        String avatar = request.get("avatar");
        String email = request.get("email");

        return authService.authenticateUser(discordId, username, discriminator, avatar, email)
                .thenApply(authResponse -> {
                    if (authResponse.isSuccess()) {
                        // Converte o User para UserDTO para garantir isAdmin no JSON
                        UserDTO userDTO = new UserDTO((User) authResponse.getUser());
                        AuthResponse responseWithDTO = new AuthResponse(
                            authResponse.isSuccess(),
                            authResponse.getMessage(),
                            authResponse.getToken(),
                            userDTO
                        );
                        return ResponseEntity.ok(ApiResponse.success(responseWithDTO));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error(authResponse.getMessage()));
                    }
                });
    }

    @PostMapping("/refresh")
    public CompletableFuture<ResponseEntity<ApiResponse<Boolean>>> refreshPermissions(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {

        String discordId = request.get("discordId");

        return authService.refreshUserPermissions(discordId)
                .thenApply(success -> ResponseEntity.ok(ApiResponse.success(success)));
    }

    @GetMapping("/discord/callback")
    public void discordCallback(@RequestParam("code") String code, HttpServletResponse response) throws Exception {
        // Troca o code pelo access token
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "client_id=" + discordClientId +
                "&client_secret=" + discordClientSecret +
                "&grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + discordRedirectUri;
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        String tokenUrl = "https://discord.com/api/oauth2/token";
        String tokenResponse = restTemplate.postForObject(tokenUrl, request, String.class);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tokenJson = mapper.readTree(tokenResponse);
        String accessToken = tokenJson.get("access_token").asText();

        // Busca dados do usuário
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<String> userRequest = new HttpEntity<>(userHeaders);
        String userUrl = "https://discord.com/api/users/@me";
        String userResponse = restTemplate.exchange(userUrl, HttpMethod.GET, userRequest, String.class).getBody();
        JsonNode userJson = mapper.readTree(userResponse);

        String discordId = userJson.get("id").asText();
        String username = userJson.get("username").asText();
        String discriminator = userJson.get("discriminator").asText();
        String avatar = userJson.has("avatar") && !userJson.get("avatar").isNull() ? userJson.get("avatar").asText() : null;
        String email = userJson.has("email") && !userJson.get("email").isNull() ? userJson.get("email").asText() : null;

        // Autentica/cria usuário e gera JWT
        AuthResponse authResponse = authService.authenticateUser(discordId, username, discriminator, avatar, email).get();
        if (!authResponse.isSuccess()) {
            response.sendRedirect("/login?error=unauthorized");
            return;
        }
        String jwt = authResponse.getToken();
        // Define cookie HTTP Only (opcional)
        ResponseCookie cookie = ResponseCookie.from("token", jwt)
                .httpOnly(true)
                .secure(Boolean.parseBoolean(System.getenv().getOrDefault("COOKIE_SECURE", "false"))) // true em produção com HTTPS
                .path("/")
                .maxAge(jwtExpiration / 1000)
                .sameSite("Lax")
                .build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        // Redireciona para login do frontend com o token na URL
        String frontendLoginUrl = System.getenv().getOrDefault("FRONTEND_LOGIN_URL", "http://localhost:3000/login");
        response.sendRedirect(frontendLoginUrl + "?token=" + jwt);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String discordId = jwtUtil.getDiscordIdFromToken(token);
            User user = userRepository.findByDiscordId(discordId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Usuário não encontrado"));
            }
            UserDTO userDTO = new UserDTO(user);
            return ResponseEntity.ok(ApiResponse.success(userDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Token inválido"));
        }
    }
}