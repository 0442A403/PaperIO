package event

data class Player(
  val territory: List<Pair<Int, Int>>,
  val position: Pair<Int, Int>,
  val direction: String?,
  val lines: List<Pair<Int, Int>>,
  val score: Int,
  val bonuses: List<ActiveBonus>,
  val distToNextPosition: Int
)