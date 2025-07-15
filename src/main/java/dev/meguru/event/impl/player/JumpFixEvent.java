package dev.meguru.event.impl.player;

import dev.meguru.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JumpFixEvent extends Event {
    private float yaw;
}
