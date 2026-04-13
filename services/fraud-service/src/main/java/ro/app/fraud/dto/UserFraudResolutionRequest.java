package ro.app.fraud.dto;

import jakarta.validation.constraints.NotBlank;

public class UserFraudResolutionRequest {

    @NotBlank
    private String resolution;

    private String notes;

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}