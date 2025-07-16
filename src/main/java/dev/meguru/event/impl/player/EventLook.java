package dev.meguru.event.impl.player;


import dev.meguru.event.Event;
import dev.meguru.utils.addons.vector.Vector2f;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventLook extends Event {
    private Vector2f rotation;

    public EventLook(Vector2f rotation) {
        this.rotation = rotation;
    }

}

