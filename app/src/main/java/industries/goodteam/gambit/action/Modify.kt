package industries.goodteam.gambit.action

import industries.goodteam.gambit.effect.Effect

class Modify(
    val effect: Effect,
    cooldown: Int = 3,
    start: Int = -1
) : Action(name = "Modify", cooldown = cooldown, start = start) {

}

