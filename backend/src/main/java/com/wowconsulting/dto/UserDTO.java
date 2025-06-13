package com.wowconsulting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wowconsulting.model.User;
import java.util.List;

public class UserDTO {
    private String id;
    private String discordId;
    private String username;
    private String discriminator;
    private String avatar;
    private String email;
    private List<String> roles;
    private boolean isAdmin;
    private boolean active;
    private String createdAt;
    private String updatedAt;

    public UserDTO(User user) {
        this.id = user.getId();
        this.discordId = user.getDiscordId();
        this.username = user.getUsername();
        this.discriminator = user.getDiscriminator();
        this.avatar = user.getAvatar();
        this.email = user.getEmail();
        this.roles = user.getRoles();
        this.isAdmin = user.isAdmin();
        this.active = user.isActive();
        this.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
        this.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;
    }

    // Getters
    public String getId() { return id; }
    public String getDiscordId() { return discordId; }
    public String getUsername() { return username; }
    public String getDiscriminator() { return discriminator; }
    public String getAvatar() { return avatar; }
    public String getEmail() { return email; }
    public List<String> getRoles() { return roles; }
    @JsonProperty("isAdmin")
    public boolean isAdmin() { return isAdmin; }
    public boolean isActive() { return active; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
