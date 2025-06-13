package com.wowconsulting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AppointmentRequest {
    @NotNull
    private String timeSlotId;

    @NotBlank
    @Size(max = 50)
    private String bnetId;

    @NotBlank
    @Size(max = 100)
    private String discordTag;

    @NotBlank
    private String characterClass;

    @NotBlank
    private String specialization;

    @NotBlank
    private String knowledgeLevel;

    @NotBlank
    @Size(max = 1000)
    private String currentContent;

    @NotBlank
    @Size(max = 1000)
    private String expectations;

    // Getters and Setters
    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }

    public String getBnetId() { return bnetId; }
    public void setBnetId(String bnetId) { this.bnetId = bnetId; }

    public String getDiscordTag() { return discordTag; }
    public void setDiscordTag(String discordTag) { this.discordTag = discordTag; }

    public String getCharacterClass() { return characterClass; }
    public void setCharacterClass(String characterClass) { this.characterClass = characterClass; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getKnowledgeLevel() { return knowledgeLevel; }
    public void setKnowledgeLevel(String knowledgeLevel) { this.knowledgeLevel = knowledgeLevel; }

    public String getCurrentContent() { return currentContent; }
    public void setCurrentContent(String currentContent) { this.currentContent = currentContent; }

    public String getExpectations() { return expectations; }
    public void setExpectations(String expectations) { this.expectations = expectations; }
}
