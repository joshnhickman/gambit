package industries.goodteam.gambit.effect

import industries.goodteam.gambit.Event
import industries.goodteam.gambit.NewRound

data class Duration(val event: Class<out Event> = NewRound::class.java, val number: Int = 0) {

}