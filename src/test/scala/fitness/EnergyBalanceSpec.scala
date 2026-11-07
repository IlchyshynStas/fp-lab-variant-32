package fitness

import java.nio.file.{Files, Paths}

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.io.Source
import scala.util.Using

class EnergyBalanceSpec extends AnyFunSuite with Matchers:

  private def readLines(path: String): List[String] =
    Using.resource(Source.fromFile(path, "UTF-8"))(_.getLines().toList)

  test("short manual sequence folds into the expected state") {
    val rows = List(
      "MEAL,oatmeal,400,,",
      "MEAL,pizza,900,,",
      "WORKOUT,run,600,45,",
      "WORKOUT,swim,300,30,",
      "BMR_TICK,8,70,,",
      "SCALE_ADJUST,-100.00,,,"
    )
    val result = Importer.run(rows)
    val state = result.state

    result.errors shouldBe empty
    state.eaten shouldBe 1300L
    state.burned shouldBe 1460L
    state.netBalance shouldBe BigDecimal(-260)
    state.trainingMinutes shouldBe 75L
    state.topWorkout.map(_.activity.value) shouldBe Some("run")

    val goal = Goal.progress(state)
    goal.actualDeficit shouldBe BigDecimal(260)
    goal.percent shouldBe BigDecimal("52.0")
    goal.remaining shouldBe BigDecimal(240)
    goal.reached shouldBe false
  }

  test("goal is reached when the deficit passes the target") {
    val result = Importer.run(List("MEAL,salad,300,,", "WORKOUT,run,900,60,"))
    val goal = Goal.progress(result.state)

    goal.reached shouldBe true
    goal.percent shouldBe BigDecimal("120.0")
    goal.remaining shouldBe BigDecimal(0)
  }

  test("first of two equal workouts stays the most expensive") {
    val result = Importer.run(List("WORKOUT,run,500,30,", "WORKOUT,swim,500,40,"))
    result.state.topWorkout.map(_.activity.value) shouldBe Some("run")
  }

  test("invalid rows become typed errors instead of exceptions") {
    val cases: List[(String, ParseError)] = List(
      "NOT_AN_EVENT,1,2,,"       -> ParseError.UnknownEvent("NOT_AN_EVENT"),
      ",,,,"                     -> ParseError.EmptyEvent,
      "MEAL,pizza,900"           -> ParseError.MalformedRow(5, 3),
      "MEAL,pizza,900,,,x"       -> ParseError.MalformedRow(5, 6),
      "MEAL,pizza,0,,"           -> ParseError.InvalidKcal("0"),
      "MEAL,pizza,3.5,,"         -> ParseError.InvalidKcal("3.5"),
      "MEAL,bad token,100,,"     -> ParseError.InvalidToken("bad token"),
      "WORKOUT,run,300,4,"       -> ParseError.InvalidDuration("4"),
      "BMR_TICK,25,70,,"         -> ParseError.InvalidHours("25"),
      "BMR_TICK,8,200,,"         -> ParseError.InvalidKcal("200"),
      "SCALE_ADJUST,0.001,,,"    -> ParseError.InvalidKcalDelta("0.001"),
      "SCALE_ADJUST,abc,,,"      -> ParseError.InvalidKcalDelta("abc"),
      "SCALE_ADJUST,NaN,,,"      -> ParseError.InvalidKcalDelta("NaN")
    )
    cases.foreach { case (row, expected) =>
      Parser.parseRow(row) shouldBe Left(expected)
    }
  }

  test("batch import skips the header, keeps going after bad rows and numbers lines from 1") {
    val lines = List(
      "event,p1,p2,p3,p4",
      "MEAL,pizza,900,,",
      "MEAL,pizza,-5,,",
      "garbage"
    )
    val result = Importer.run(lines)

    result.events.size shouldBe 1
    result.errors.map(_.line) shouldBe List(3, 4)
    result.errors.map(_.error) shouldBe List(ParseError.InvalidKcal("-5"), ParseError.MalformedRow(5, 1))
    result.state.eaten shouldBe 900L
  }

  test("first 1000 rows of the generated variant 32 file") {
    val result = Importer.run(readLines("testdata/sample_32.csv"))
    val state = result.state

    result.events.size shouldBe 947
    result.errors.size shouldBe 53
    state.eaten shouldBe 281402L
    state.workoutBurned shouldBe 177378L
    state.basalBurned shouldBe 96598L
    state.adjustment shouldBe BigDecimal("-3848.85")
    state.netBalance shouldBe BigDecimal("3577.15")
    state.trainingMinutes shouldBe 13863L
    state.topWorkout.map(w => (w.activity.value, w.kcal.value)) shouldBe Some(("O4DMTWI3XSL6", 1191))
  }

  test("full generated file with 500000 rows") {
    val path = "testdata/variant_32_fitness_calories.csv"
    assume(Files.exists(Paths.get(path)))
    val result = Importer.run(readLines(path))

    (result.events.size + result.errors.size) shouldBe 500000
    result.events.size shouldBe 474889
    result.errors.size shouldBe 25111
    result.state.netBalance shouldBe BigDecimal("-6658870.52")
    result.state.trainingMinutes shouldBe 7144782L
  }
