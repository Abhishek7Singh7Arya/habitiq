package com.habitiq.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoutineConfirmedEvent extends BaseEvent {
    private String userId;
    private String routineId;
    private String conversationId;
}
