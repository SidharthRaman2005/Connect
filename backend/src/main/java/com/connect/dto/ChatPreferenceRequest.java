package com.connect.dto;

/** Per-conversation preferences. Null fields mean "leave unchanged". */
public record ChatPreferenceRequest(Boolean muted, Boolean archived) {}
