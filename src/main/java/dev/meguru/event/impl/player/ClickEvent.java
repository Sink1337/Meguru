package dev.meguru.event.impl.player;

import dev.meguru.event.Event;

public class ClickEvent extends Event {
    boolean fake;

    public ClickEvent(boolean fake) {
        this.fake = fake;
    }

    public boolean isFake() {
        return fake;
    }
}
