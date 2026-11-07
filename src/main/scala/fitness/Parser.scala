package fitness

object Parser:
  val Header = "event,p1,p2,p3,p4"
  private val ColumnCount = 5

  def parseRow(raw: String): Either[ParseError, Event] =
    val cols = raw.split(",", -1)
    if cols.length != ColumnCount then Left(ParseError.MalformedRow(ColumnCount, cols.length))
    else parseEvent(cols)

  private def parseEvent(cols: Array[String]): Either[ParseError, Event] =
    cols(0) match
      case "" => Left(ParseError.EmptyEvent)
      case "MEAL" =>
        for
          label <- Label.from(cols(1))
          kcal  <- Kcal.from(cols(2))
        yield Event.Meal(label, kcal)
      case "WORKOUT" =>
        for
          activity <- Label.from(cols(1))
          kcal     <- Kcal.from(cols(2))
          minutes  <- Minutes.from(cols(3))
        yield Event.Workout(WorkoutRecord(activity, kcal, minutes))
      case "BMR_TICK" =>
        for
          hours <- Hours.from(cols(1))
          rate  <- KcalPerHour.from(cols(2))
        yield Event.BmrTick(hours, rate)
      case "SCALE_ADJUST" =>
        KcalDelta.from(cols(1)).map(Event.ScaleAdjust(_))
      case other =>
        Left(ParseError.UnknownEvent(other))
