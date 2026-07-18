package com.habitiq.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WhatsAppResponseEvent extends BaseEvent {
    private String fromPhone;
    private String messageBody;
    private String matchedTaskId;
    private String responseType;
}
