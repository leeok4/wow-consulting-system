package com.wowconsulting.service;

import com.wowconsulting.model.User;
import com.wowconsulting.repository.UserRepository;
import com.wowconsulting.dto.AuthResponse;
import com.wowconsulting.util.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private JwtUtil jwtUtil;

    public CompletableFuture<AuthResponse> authenticateUser(String discordId, String username, String discriminator, String avatar, String email) {
        return discordService.isUserInGuildWithRole(discordId)
                .thenCompose(hasRole -> {
                    if (!hasRole) {
                        return CompletableFuture.completedFuture(
                                new AuthResponse(false, "Usuário não possui os cargos necessários", null, null)
                        );
                    }

                    return discordService.isUserAdmin(discordId)
                            .thenCompose(isAdmin -> {
                                return discordService.getUserRoles(discordId)
                                        .thenApply(roles -> {
                                            User user = findOrCreateUser(discordId, username, discriminator, avatar, email, roles, isAdmin);
                                            String token = jwtUtil.generateToken(user.getDiscordId());

                                            return new AuthResponse(true, "Autenticação realizada com sucesso", token, user);
                                        });
                            });
                });
    }

    private User findOrCreateUser(String discordId, String username, String discriminator, String avatar, String email, List<String> roles, boolean isAdmin) {
        Optional<User> existingUser = userRepository.findByDiscordId(discordId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Atualiza informações do usuário
            user.setUsername(username);
            user.setDiscriminator(discriminator);
            user.setAvatar(avatar);
            user.setEmail(email);
            user.setRoles(roles);
            user.setAdmin(isAdmin);
        } else {
            // Cria novo usuário
            user = new User(discordId, username, discriminator);
            user.setAvatar(avatar);
            user.setEmail(email);
            user.setRoles(roles);
            user.setAdmin(isAdmin);
        }

        return userRepository.save(user);
    }

    public Optional<User> validateToken(String token) {
        try {
            if (jwtUtil.isTokenExpired(token)) {
                return Optional.empty();
            }

            String discordId = jwtUtil.getDiscordIdFromToken(token);
            return userRepository.findByDiscordId(discordId);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public CompletableFuture<Boolean> refreshUserPermissions(String discordId) {
        return discordService.isUserInGuildWithRole(discordId)
                .thenCompose(hasRole -> {
                    if (!hasRole) {
                        // Desativa o usuário se não tiver mais os cargos
                        Optional<User> userOpt = userRepository.findByDiscordId(discordId);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            user.setActive(false);
                            userRepository.save(user);
                        }
                        return CompletableFuture.completedFuture(false);
                    }

                    return discordService.getUserRoles(discordId)
                            .thenCompose(roles -> {
                                return discordService.isUserAdmin(discordId)
                                        .thenApply(isAdmin -> {
                                            Optional<User> userOpt = userRepository.findByDiscordId(discordId);
                                            if (userOpt.isPresent()) {
                                                User user = userOpt.get();
                                                user.setRoles(roles);
                                                user.setAdmin(isAdmin);
                                                user.setActive(true);
                                                userRepository.save(user);
                                            }
                                            return true;
                                        });
                            });
                });
    }
}