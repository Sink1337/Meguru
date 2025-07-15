package dev.merguru.event.impl.player;

import dev.merguru.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JumpFixEvent extends Event {
    private float yaw;
}
