package industries.goodteam.gambit.actor

import industries.goodteam.gambit.Stat
import industries.goodteam.gambit.Stat.*
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.action.Nothing
import industries.goodteam.gambit.effect.AppliedEffect

open class Actor(
    var name: String,
    var luck: Int = 1,
    var vitality: Int = 30,
    var strength: Int = 5,
    var accuracy: Int = 1,
    var armor: Int = 5,
    var reflexes: Int = 1,
    var concentration: Int = 1,
    vararg var actions: Action
) {

    val nothing = Nothing()

    init {
        actions.forEach { it.actor = this }
        nothing.actor = this
    }

    var health = vitality
    var shield = 0

    var gold = 0

    var intent = actions[0]
    var attackMultiplier = 1

    var stunLeft = -1

    var effects = mutableListOf<AppliedEffect>()

    var acted = false


    open fun act(action: Action = intent) {
        if (!acted) {
            action.perform()
            acted = true
        }
    }

    fun endRound() {
        shield = 0
        actions.forEach { it.endRound() }
        effects.removeAll {
            it.update()
            if (it.done()) modifyStat(it.targetStat, -it.value)
            it.done()
        }
        if (stunned()) stunLeft--
        acted = false
    }

    fun endCombat() {
        actions.forEach { it.endCombat() }
    }

    open fun intend(action: Action? = null) {
        intent = when {
            stunned() -> nothing
            action != null -> action
            else -> actions.filter { it.ready() }.random()
        }
    }

    open fun defend(): Int {
        val defenceValue = actionValue(Defend()).random()
        shield += defenceValue
        return defenceValue
    }

    open fun damage(amount: Int): Int {
        var actualAmount = amount - shield
        if (actualAmount < 0) actualAmount = 0
        if (health - actualAmount >= 0) health -= actualAmount else health = 0
        return actualAmount
    }

    open fun heal(amount: Int = vitality): Int {
        var actualAmount = amount
        if (health + amount > vitality) actualAmount = vitality - health
        health += actualAmount
        return actualAmount
    }

    open fun stun(duration: Int): Int {
        stunLeft = duration
        actions.forEach { it.left = stunLeft }
        intend(Nothing())
        return duration
    }

    open fun affect(effect: AppliedEffect) {
        effects.add(effect)
        modifyStat(effect.targetStat, effect.value)

    }

    private fun modifyStat(stat: Stat, value: Int) {
        when (stat) {
            LUCK -> luck += value
            VITALITY -> vitality += value
            HEALTH -> health += value
            STRENGTH -> strength += value
            ACCURACY -> accuracy += value
            ARMOR -> armor += value
            REFLEXES -> reflexes += value
            CONCENTRATION -> concentration += value
        }
    }

    fun stat(stat: Stat): Int = when (stat) {
        LUCK -> luck
        VITALITY -> vitality
        HEALTH -> health
        STRENGTH -> strength
        ACCURACY -> accuracy
        ARMOR -> armor
        REFLEXES -> reflexes
        CONCENTRATION -> concentration
    }

    open fun alive(): Boolean = health > 0

    open fun stunned(): Boolean = stunLeft > -1

    // TODO: remove when actions handle this
    fun actionValue(action: Action = intent): IntRange = when (action) {
        is Attack -> (if (accuracy < strength) accuracy * attackMultiplier else strength * attackMultiplier)..strength * attackMultiplier
        is Defend -> (if (reflexes < armor) reflexes else armor)..armor
        is Stun -> concentration..concentration
        is Steal -> accuracy * 10..reflexes * 10
        is Nothing -> stunLeft..stunLeft
        is Modify -> action.effect.value..action.effect.value
    }

}