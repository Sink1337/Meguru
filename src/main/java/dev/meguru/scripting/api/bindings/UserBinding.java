package dev.meguru.scripting.api.bindings;

import dev.meguru.Meguru;
import store.intent.intentguard.annotation.Exclude;
import store.intent.intentguard.annotation.Strategy;

@Exclude(Strategy.NAME_REMAPPING)
public class UserBinding {

    public String uid() {
        return String.valueOf(Meguru.INSTANCE.getIntentAccount().client_uid);
    }

    public String username() {
        return String.valueOf(Meguru.INSTANCE.getIntentAccount().username);
    }

    public String discordTag() {
        return String.valueOf(Meguru.INSTANCE.getIntentAccount().discord_tag);
    }

}
