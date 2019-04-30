package industries.goodteam.gambit.actor

import industries.goodteam.gambit.StatType
import industries.goodteam.gambit.StatType.*
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.action.Nothing
import industries.goodteam.gambit.action.Target
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

    var health = vitality
    var shield = 0

    var gold = 0

    var intent = actions[0]
    var attackMultiplier = 1

    private var stunLeft = -1

    private var effects = mutableListOf<AppliedEffect>()

    open fun update() {
        shield = 0

        actions.forEach { it.update() }

        effects.removeAll {
            it.update()
            if (it.done()) modifyStat(it.targetStat, -it.value)
            it.done()
        }

        if (stunned()) stunLeft--
    }

    open fun endCombat() {
        actions.forEach { it.refresh() }
    }

    open fun intend(action: Action? = null) {
        intent = when {
            stunned() -> Nothing()
            action != null -> action
            else -> actions.filter { it.ready() }.random()
        }
    }

    open fun act(action: Action = intent) {
//        val target = if (action.target == Target.SELF) this else
        action.perform()
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

    private fun modifyStat(stat: StatType, value: Int) {
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

    fun stat(stat: StatType): Int = when(stat) {
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

    fun actionValue(action: Action = intent): IntRange {
        return when (action) {
            is Attack -> (if (accuracy < strength) accuracy * attackMultiplier else strength * attackMultiplier)..strength * attackMultiplier
            is Defend -> (if (reflexes < armor) reflexes else armor)..armor
            is Stun -> concentration..concentration
            is Steal -> accuracy * 10..reflexes * 10
            is Nothing -> stunLeft..stunLeft
            is Modify -> action.effect.value..action.effect.value
        }
    }

}