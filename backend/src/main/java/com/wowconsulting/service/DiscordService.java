package com.wowconsulting.service;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DiscordService {

    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.bot.guild-id}")
    private String guildId;

    @Value("#{'${discord.bot.required-roles}'.split(',')}")
    private List<String> requiredRoles;

    @Value("#{'${discord.bot.admin-roles}'.split(',')}")
    private List<String> adminRoles;

    private JDA jda;
    private Guild guild;

    @PostConstruct
    public void init() {
        try {
            this.jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES)
                    .build();

            this.jda.awaitReady();
            this.guild = jda.getGuildById(guildId);

            if (guild == null) {
                throw new RuntimeException("Guild não encontrada com ID: " + guildId);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao inicializar Discord Bot", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public CompletableFuture<Boolean> isUserInGuildWithRole(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Member member = guild.getMemberById(userId);
                if (member == null) {
                    // Tenta buscar via API se não estiver em cache
                    try {
                        member = guild.retrieveMemberById(userId).complete();
                        // System.out.println("[DEBUG] Membro buscado via API: " + (member != null));
                    } catch (Exception ex) {
                        // System.out.println("[DEBUG] Erro ao buscar membro via API: " + ex.getMessage());
                    }
                }
                if (member == null) {
                    // System.out.println("[DEBUG] Servidor de consulta: " + guildId);
                    // System.out.println("[DEBUG] Membro não encontrado no servidor: " + userId);
                    return false;
                }
                List<Role> memberRoles = member.getRoles();
                boolean hasRole = memberRoles.stream()
                        .anyMatch(role -> requiredRoles.contains(role.getId()));
                // System.out.println("[DEBUG] Roles do usuário: " + memberRoles.stream().map(Role::getId).toList());
                // System.out.println("[DEBUG] requiredRoles: " + requiredRoles);
                return hasRole;
            } catch (Exception e) {
                System.err.println("[DEBUG] Erro ao verificar cargos: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isUserAdmin(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Member member = guild.getMemberById(userId);
                if (member == null) {
                    // Tenta buscar via API se não estiver em cache
                    try {
                        member = guild.retrieveMemberById(userId).complete();
                        // System.out.println("[DEBUG] Membro buscado via API (admin): " + (member != null));
                    } catch (Exception ex) {
                        // System.out.println("[DEBUG] Erro ao buscar membro via API (admin): " + ex.getMessage());
                    }
                }
                if (member == null) {
                    // System.out.println("[DEBUG] Membro não encontrado no servidor (admin): " + userId);
                    return false;
                }
                List<Role> memberRoles = member.getRoles();
                boolean isAdmin = memberRoles.stream()
                        .anyMatch(role -> adminRoles.contains(role.getId()));
                // System.out.println("[DEBUG] Roles do usuário (admin): " + memberRoles.stream().map(Role::getId).toList());
                // System.out.println("[DEBUG] adminRoles: " + adminRoles);
                return isAdmin;
            } catch (Exception e) {
                System.err.println("[DEBUG] Erro ao verificar cargos admin: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<List<String>> getUserRoles(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Member member = guild.getMemberById(userId);
                if (member == null) {
                    // Tenta buscar via API se não estiver em cache
                    try {
                        member = guild.retrieveMemberById(userId).complete();
                        // System.out.println("[DEBUG] Membro buscado via API (roles): " + (member != null));
                    } catch (Exception ex) {
                        // System.out.println("[DEBUG] Erro ao buscar membro via API (roles): " + ex.getMessage());
                    }
                }
                if (member == null) {
                    // System.out.println("[DEBUG] Membro não encontrado ao buscar roles: " + userId);
                    return List.of();
                }
                return member.getRoles().stream()
                        .map(Role::getId)
                        .toList();
            } catch (Exception e) {
                System.err.println("[DEBUG] Erro ao buscar roles: " + e.getMessage());
                return List.of();
            }
        });
    }

    public CompletableFuture<Void> sendAppointmentNotification(String appointmentInfo) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Procura por um canal de notificações ou canal geral
                TextChannel channel = guild.getTextChannelsByName("agendamentos", true).stream()
                        .findFirst()
                        .orElse(guild.getDefaultChannel().asTextChannel());

                if (channel != null) {
                    channel.sendMessage("🗓️ **Nova Consultoria Agendada!**\n" + appointmentInfo).queue();
                }
            } catch (Exception e) {
                // Log do erro, mas não falha o processo
                System.err.println("Erro ao enviar notificação: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> sendAppointmentReminder(String userId, String appointmentInfo) {
        return CompletableFuture.runAsync(() -> {
            try {
                Member member = guild.getMemberById(userId);
                if (member != null) {
                    member.getUser().openPrivateChannel().queue(channel -> {
                        channel.sendMessage("⏰ **Lembrete de Consultoria**\n" + appointmentInfo).queue();
                    });
                }
            } catch (Exception e) {
                System.err.println("Erro ao enviar lembrete: " + e.getMessage());
            }
        });
    }

    public CompletableFuture<Void> sendAppointmentToOwner(String appointmentInfo) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (guild != null && guild.getOwner() != null) {
                    guild.getOwner().getUser().openPrivateChannel().queue(channel -> {
                        channel.sendMessage("🗓️ **Novo agendamento criado!**\n" + appointmentInfo).queue();
                    });
                }
            } catch (Exception e) {
                System.err.println("Erro ao enviar notificação ao dono: " + e.getMessage());
            }
        });
    }
}