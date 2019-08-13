package event

data class Params(
  val me: Player,
  val players: List<Player>,
  val tick_num: Int,
  val bonuses: List<Bonus>
)