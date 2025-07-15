package dev.merguru.utils;


import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.status.server.S01PacketPong;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlinkUtils implements Utils {

    public static BlinkUtils instance = new BlinkUtils();
    public LinkedBlockingDeque<Packet<?>> packets = new LinkedBlockingDeque<>();
    public boolean pass = false;
    public boolean blinking = false;
    public int blinkTicks = 0;
    public int delayTicks = 0;
    private List<Consumer<Packet<?>>> releaseConsumers = new ArrayList<>();
    private List<Consumer<Packet<?>>> handleConsumers = new ArrayList<>();
    private List<Predicate<Packet<?>>> handlePredicate = new ArrayList<>();
    private BlinkUtils() {

    }

    private void reset(){
        blinkTicks = 0;
        delayTicks = 0;

        pass = false;
        blinking = false;

        packets.clear();
        handleConsumers.clear();
        handlePredicate.clear();
        releaseConsumers.clear();
    }

    public boolean add(Packet<?> packet) {
        if(mc.thePlayer == null) {
            reset();
            return false;
        }
        if (packet instanceof C0FPacketConfirmTransaction) {
            System.out.println("C0FPacketConfirmTransaction");
        }
        for (Predicate<Packet<?>> predicate : handlePredicate) {
            if (predicate.test(packet)) {
                return false;
            }
        }
        handleConsumers.forEach(consumer -> consumer.accept(packet));
        packets.add(packet);
        return true;
    }

    public void onTick(){
        if (blinking){
            blinkTicks++;
            delayTicks++;
            packets.add(new S01PacketPong());
        }
        if(mc.thePlayer == null) {
            reset();
        }
    }

    public void release(){
        release(Integer.MAX_VALUE);
    }

    public void release(int ticks) {
        while (!instance.packets.isEmpty()) {
            if (ticks == 0)
                break;
            Packet<?> packet = instance.packets.poll();

            if (packet instanceof S01PacketPong) {
                delayTicks--;
                ticks--;
                continue;
            }

            releaseConsumers.forEach(consumer -> consumer.accept(packet));

            sendPacket(packet);
        }
    }

    public void sendPacket(Packet<?> packet){
        instance.pass = true;
        try {
            mc.getNetHandler().getNetworkManager().sendPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        instance.pass = false;
    }

    public static void startBlink() {
        instance.reset();
        instance.blinking = true;
    }

    public static void stopBlink() {
        instance.release();
        instance.reset();
    }


    public static void releaseTick(int ticks) {
        instance.release(ticks);
    }

    public static void addFilter(Predicate<Packet<?>> predicate) {
        instance.handlePredicate.add(predicate);
    }

    public static void addHandleAction(Consumer<Packet<?>> packetConsumer) {
        instance.handleConsumers.add(packetConsumer);
    }

    public static void addReleaseConsumer(Consumer<Packet<?>> packetConsumer) {
        instance.releaseConsumers.add(packetConsumer);
    }
}
