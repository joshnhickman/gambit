package industries.goodteam.gambit

enum class Action {

    ATTACK,
    DEFEND,
    UTILITY;

    companion object {
        fun random(): Action = Action.values()[(0..2).random()]

        fun not(action: Action): Action {
            var newAction: Action
            do {
                newAction = random()
            } while (newAction == action)
            return newAction
        }
    }

}