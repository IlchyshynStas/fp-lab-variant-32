package fitness

final case class LineError(line: Int, raw: String, error: ParseError)

final case class ImportResult(errors: List[LineError], events: List[Event], state: EnergyState)

object Importer:
  def parseAll(lines: List[String]): (List[LineError], List[Event]) =
    lines.zipWithIndex
      .filterNot { case (line, _) => line == Parser.Header }
      .partitionMap { case (line, index) =>
        Parser.parseRow(line).left.map(error => LineError(index + 1, line, error))
      }

  def run(lines: List[String]): ImportResult =
    val (errors, events) = parseAll(lines)
    ImportResult(errors, events, events.foldLeft(EnergyState.empty)(EnergyState.update))
