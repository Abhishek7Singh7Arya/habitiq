package com.habitiq.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TaskStatusUpdatedEvent extends BaseEvent {
    private String userId;
    private String taskId;
    private String status;
    private String notes;
}
