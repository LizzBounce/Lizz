package net.ccbluex.liquidbounce.utils.client;

import me.utils.WebUtil;
import net.ccbluex.liquidbounce.Lizz;

import java.awt.*;
import java.io.IOException;

import static net.ccbluex.liquidbounce.utils.client.MinecraftInstance.mc;

public class NoC03 {

    public static String GetSB() {
        String a = "L";
        String b = "i";
        String c = "z";
        String d = "z";
        return a + b + c + d;
    }

    public static void CheckMom() throws IOException {
        String a = WebUtil.get("https://clientcdn.bmwcloud.top/BMWClient/JBY.txt");
        if (a.contains(GetSB())) {
            ClientUtils.INSTANCE.getLOGGER().info("[Mum Checker] Good");
        } else {
            Lizz.INSTANCE.displayTray("Mum Checker", "改你妈妈的字符串 你是傻逼吗", TrayIcon.MessageType.ERROR);
            mc.shutdown();
        }
    }
}
