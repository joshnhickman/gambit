package industries.goodteam.gambit.effect

import industries.goodteam.gambit.Stat

// TODO: make this abstract for other effects
class Effect(
    val targetStat: Stat = Stat.STRENGTH,
    val value: Int = -1,
//    val duration: Duration
    val duration: Int = 0
) {

//    constructor(targetStat: Stat = Stat.STRENGTH, value: Int = -1, duration: Int = 1)
//            : this(targetStat, value, Duration(number = 1))

    fun apply(): AppliedEffect = AppliedEffect(this)

}