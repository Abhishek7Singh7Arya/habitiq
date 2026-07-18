package com.habitiq.common.kafka;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String USER_REGISTERED          = "user.registered";
    public static final String ROUTINE_CONFIRMED        = "routine.confirmed";
    public static final String FILE_PARSED              = "file.parsed";
    public static final String TASK_DUE                 = "task.due";
    public static final String TASK_STATUS_UPDATED      = "task.status.updated";
    public static final String WHATSAPP_RESPONSE        = "whatsapp.response.received";
    public static final String VOICE_RESPONSE           = "voice.response.received";
    public static final String PROGRESS_UPDATED         = "progress.updated";
}
