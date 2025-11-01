/*
 * Lizz Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/LizzBounce/Lizz/
 */
package net.ccbluex.liquidbounce.features.command.shortcuts

open class Token

class Literal(val literal: String) : Token()

class StatementEnd : Token()
