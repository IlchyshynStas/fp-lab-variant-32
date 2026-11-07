package fitness

final case class GoalProgress(
    targetDeficit: Int,
    actualDeficit: BigDecimal,
    remaining: BigDecimal,
    percent: BigDecimal,
    reached: Boolean
)

object Goal:
  val DailyDeficitTarget = 500

  def progress(state: EnergyState, target: Int = DailyDeficitTarget): GoalProgress =
    val deficit = -state.netBalance
    val percent = (deficit * 100 / BigDecimal(target)).setScale(1, BigDecimal.RoundingMode.HALF_UP)
    GoalProgress(
      targetDeficit = target,
      actualDeficit = deficit,
      remaining = (BigDecimal(target) - deficit).max(BigDecimal(0)),
      percent = percent,
      reached = deficit >= BigDecimal(target)
    )
