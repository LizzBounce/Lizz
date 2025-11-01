package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils

object JumpReset : Module("JumpReset", Category.COMBAT) {
    val hurttime by int("HurtTime", 10, 0..10) {!randomHurttime}

    val randomHurttime by boolean("RandomHurtTime", false)
    var polarhurttime = hurttime

    override fun onEnable() {
        polarhurttime = if (randomHurttime) {
            RandomUtils.nextInt(8, 10)
        } else {
            hurttime
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        if (thePlayer.isInLiquid || thePlayer.isInWeb || thePlayer.isDead)
            return@handler

        if (thePlayer.hurtTime == polarhurttime) {
            thePlayer.tryJump()

            polarhurttime = if (randomHurttime) {
                RandomUtils.nextInt(8,10)
            } else {
                hurttime
            }
        }
    }
}