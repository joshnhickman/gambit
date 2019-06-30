package industries.goodteam.gambit.actor

import industries.goodteam.gambit.action.*

class Player(
    luck: Int,
    vitality: Int,
    strength: Int,
    accuracy: Int,
    armor: Int,
    reflexes: Int,
    concentration: Int,
    val attack: Attack,
    val defend: Defend,
    val stun: Stun,
    val steal: Steal
) : Actor(
    name = "player",
    luck = luck,
    vitality = vitality,
    strength = strength,
    accuracy = accuracy,
    armor = armor,
    reflexes = reflexes,
    concentration = concentration,
    actions = listOf(attack, defend, stun, steal)
) {

    // temporary until multiplier is turned into an effect so that actor can be final
    init {
        actions.forEach { it.actor = this }
        nothing.actor = this
    }

    override fun act(action: Action) {
        super.act(action)
        if (action is Attack) {
            if (attackMultiplier < 3) attackMultiplier++
        } else attackMultiplier = 1
    }

}