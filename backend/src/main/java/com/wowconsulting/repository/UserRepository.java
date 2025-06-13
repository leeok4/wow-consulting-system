package com.wowconsulting.repository;

import com.wowconsulting.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByDiscordId(String discordId);
    List<User> findByIsActiveTrue();
    List<User> findByIsAdminTrue();
}