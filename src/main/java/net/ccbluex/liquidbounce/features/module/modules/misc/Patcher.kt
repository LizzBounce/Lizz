package net.ccbluex.liquidbounce.features.module.modules.misc

import me.utils.Sound
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import org.lwjgl.opengl.Display


object Patcher: Module("Patcher", Category.MISC, forcedDescription = "improving your experience without bloatware, aka. Essential.") {

    
    val tabOutReduceFPS by boolean("ReduceFPSWhenNoFocus", false)

    
    val chatPosition by boolean("ChatPosition1.12", true)
    


    var oldFPS: Int = 0

    override fun onEnable() {
        oldFPS = Sound.mc.gameSettings.limitFramerate
    }

    val onUpdate = handler<UpdateEvent> {
        if (tabOutReduceFPS) {
            if (!Display.isActive()) {
                Sound.mc.gameSettings.limitFramerate = 3
            } else if (Display.isActive()) {
                Sound.mc.gameSettings.limitFramerate = oldFPS
            }
        }
    }

}