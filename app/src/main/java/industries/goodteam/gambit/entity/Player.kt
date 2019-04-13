package industries.goodteam.gambit.entity

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
): Entity(
    name = "player",
    luck = luck,
    vitality = vitality,
    strength = strength,
    accuracy = accuracy,
    armor = armor,
    reflexes = reflexes,
    concentration = concentration,
    actions = *arrayOf(attack, defend, stun, steal)
) {

    override fun act(action: Action) {
        super.act(action)
        if (action is Attack) {
            if (attackMultiplier < 3) attackMultiplier++
        } else attackMultiplier = 1
    }

}