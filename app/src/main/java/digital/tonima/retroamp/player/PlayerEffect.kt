package digital.tonima.retroamp.player

sealed interface PlayerEffect {
    data class Error(val message: String) : PlayerEffect
    data class ShowMessage(val message: String) : PlayerEffect
}
