package com.habitiq.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskDueEvent extends BaseEvent {
    private String userId;
    private String taskId;
    private String taskType;
    private String description;
    private String scheduledTime;
    private String phone;
    private String name;
}
