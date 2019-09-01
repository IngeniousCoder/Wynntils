/*
 *  * Copyright © Wynntils - 2019.
 */

package com.wynntils.modules.core.instances;

import com.wynntils.core.utils.Utils;
import com.wynntils.modules.core.managers.PingManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

import java.util.function.Function;

public class PacketResponse {

    Packet input;
    Class responseType;

    Function<Packet, Boolean> verification = null;
    Runnable onDrop = null;
    Runnable onBeforeSend = null;
    Runnable onAfterSend = null;
    boolean skipping = false;

    long lastSent = -1;
    int tries = 0;
    int maxTries = 3;

    public PacketResponse(Packet input, Class responseType) {
        this.input = input;
        this.responseType = responseType;
    }

    public PacketResponse(Packet input) {
        this.input = input;

        this.responseType = null;
    }

    public Packet getInput() {
        return input;
    }

    public Class getResponseType() {
        return responseType;
    }

    public PacketResponse setVerification(Function<Packet, Boolean> verification) {
        this.verification = verification;
        return this;
    }

    public boolean shouldSend() {
        return lastSent == -1 || System.currentTimeMillis() - lastSent > PingManager.getLastPing() + 150;
    }

    //TODO make this verification faster cuz at the current state it's slowing the packet a lot
    public boolean isResponseValid(Packet packetType) {
        if (skipping) {
            return true;
        }
        if(responseType == null || tries >= maxTries) {
            if (this.onDrop != null) this.onDrop.run();
            return true; //this avoids packet spamming
        }
        if(!packetType.getClass().isAssignableFrom(responseType)) return false;

        return verification == null || verification.apply(packetType);
    }

    public void sendPacket() {
        if (skipping || !shouldSend()) return;

        Utils.runAsync(() -> {
            if (this.onBeforeSend != null) this.onBeforeSend.run();
            Minecraft.getMinecraft().getConnection().sendPacket(input);
            if (this.onAfterSend != null) this.onAfterSend.run();
            lastSent = System.currentTimeMillis();
            tries++;
        });
    }

    /**
     * Called when the packet is dropped because the verification hasn't passed
     */
    public PacketResponse onDrop(Runnable onDrop) {
        this.onDrop = onDrop;
        return this;
    }

    /**
     * Call to not send this packet. Does not call onDrop.
     */
    public void skip() {
        skipping = true;
    }

    /**
     * Called just before the packet is sent every time it is actually sent (On the same tick/thread)
     */
    public PacketResponse beforeSend(Runnable beforeSend) {
        this.onBeforeSend = beforeSend;
        return this;
    }

    /**
     * Called just after the packet is sent every time it is actually sent (On the same tick/thread)
     */
    public PacketResponse afterSend(Runnable afterSend) {
        this.onAfterSend = afterSend;
        return this;
    }

    /**
     * Set the maximum number of times to try to send this packet
     */
    public PacketResponse withMaxTries(int maxTries) {
        this.maxTries = maxTries;
        return this;
    }

}