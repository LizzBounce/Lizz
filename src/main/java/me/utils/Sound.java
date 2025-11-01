package me.utils;

import net.minecraft.client.Minecraft;


public class Sound {
    public static Sound INSTANCE = new Sound();
    public static final Minecraft mc = Minecraft.getMinecraft();
    private static boolean notificationsAllowed = false;
    public static void notificationsAllowed(boolean value) {
        notificationsAllowed = value;
    }
    public void Spec(){
        new SoundPlayer().playSound(SoundPlayer.SoundType.SPECIAL, 15F);
    }

    public void Enable() {
        new SoundPlayer().playSoundatFile("enable.wav", 10F);
    }

    public void Disable() {
        new SoundPlayer().playSoundatFile("disable.wav", 10F);
    }
}

