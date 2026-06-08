package com.team08.backend.domain.study.entity;

public enum StudyStatus {
    READY,
    IN_PROGRESS,
    CLOSED;

    public boolean isEditable() {
        return this != CLOSED;
    }
}
