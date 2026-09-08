package com.FirstApiChallenge.api.enums;

public enum NotificationType {
    LINK_REQUEST_SENT,      // Para o Veterinário ao enviar
    LINK_REQUEST_RECEIVED,  // Para o Tutor ao receber
    LINK_REQUEST_ACCEPTED,  // Quando o Tutor aceita
    LINK_REQUEST_REJECTED   // Quando o Tutor recusa
}