package com.FirstApiChallenge.api.dto;

import java.util.List;

public record ClyvoAiRequestDTO(
        String question,
        String actorType,
        AiAnimalContextDTO animal,
        List<AiMedicalRecordContextDTO> medicalRecords,
        List<AiPrescriptionContextDTO> prescriptions,
        List<AiExamContextDTO> exams,
        List<AiVaccinationContextDTO> vaccinations,
        List<AiAppointmentContextDTO> appointments
) {
}
