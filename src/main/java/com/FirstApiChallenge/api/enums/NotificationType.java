package com.FirstApiChallenge.api.enums;

public enum NotificationType {
    LINK_REQUEST_SENT,      // Para o Veterinário ao enviar
    LINK_REQUEST_RECEIVED,  // Para o Tutor ao receber
    LINK_REQUEST_ACCEPTED,  // Quando o Tutor aceita
    LINK_REQUEST_REJECTED,  // Quando o Tutor recusa
    APPOINTMENT_REQUESTED,
    APPOINTMENT_CONFIRMED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_COMPLETED,
    MEDICAL_RECORD_CREATED,
    PRESCRIPTION_CREATED,
    EXAM_REQUESTED,
    EXAM_RESULT_AVAILABLE,
    EXAM_CANCELLED
}
