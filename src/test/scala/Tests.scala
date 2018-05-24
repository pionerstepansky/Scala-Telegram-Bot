import main.scala._
import org.scalatest.FlatSpec

class Tests extends FlatSpec {
  val dateParser = new java.text.SimpleDateFormat("hh:mm:ss yy:MM:dd")

  "After initialization poll repo's map" should "be empty" in {
    assert(PollRepo().polls.isEmpty)
  }

  "After Initialization poll repo's context id" should "be zero" in {
    assertResult(0) {
      PollRepo().currentContextPoll
    }
  }

  "Poll repo" should "contains new poll after create poll" in {
    val (_, actual) = CreatePoll("test") execute PollRepo()
    assert(actual.polls.nonEmpty)
    assertResult("test") {
      actual.polls(1).getName
    }
  }

  "When poll repo is empty Listing" should "be empty" in {
    val (message, _) = Listing() execute PollRepo()
    assert(message.isEmpty)
  }

  "Poll repo" should "delete poll if it contains" in {
    val (_, ctx) = CreatePoll("test") execute PollRepo()
    val id = ctx.polls.keys.head
    val (_, actual) = DeletePoll(id) execute ctx
    assert(actual.polls.isEmpty)
  }

  "Poll repo" should "not delete anything when delete incorrect poll id" in {
    val (_, ctx) = CreatePoll("test") execute PollRepo()
    val (_, actual) = DeletePoll(1000) execute ctx
    assert(actual.polls.nonEmpty)
  }

  "Poll Repo" should "not start poll, when it's running" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 18:03:16"))) execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (message, _) = StartPoll(id) execute ctx
    assertResult("Error: The poll is already on")(message)
  }

  "Poll Repo" should "start poll, when start time is undefined" in {
    val (msg, ctx) = CreatePoll("HI") execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (message, _) = StartPoll(id) execute ctx
    assertResult("Ok: The poll was launched")(message)
  }

  "Poll Repo" should "not start poll, when start time is defined" in {
    val (msg, ctx) = CreatePoll("HI", dateStart =
      Some(dateParser.parse("12:05:30 19:03:16"))) execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (message, _) = StartPoll(id) execute ctx
    assertResult("Error: Start time is already defined")(message)
  }

  "Poll Repo" should "not start poll, when it's finished" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 16:03:16")),
      dateEnd = Some(dateParser.parse("12:05:30 18:05:16"))) execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (message, _) = StartPoll(id) execute ctx
    assertResult("Error: Poll is finished")(message)
  }

  "Poll Repo" should "not stop poll, when it doesn't running" in {
    val (msg, ctx) = CreatePoll("HI") execute PollRepo()
    val (message, _) = StopPoll(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Error: Poll is already off")(message)
  }

  "Poll Repo" should "not stop poll, when stop time is defined" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 18:03:16")),
      dateEnd = Some(dateParser.parse("12:05:30 18:05:16"))) execute PollRepo()
    val (message, _) = StopPoll(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Error: Stop time is already defined")(message)
  }

  "Poll Repo" should "stop poll, when poll is running and stop time isn't defined" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 18:03:16"))) execute PollRepo()
    val (message, _) = StopPoll(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Ok: The poll is over")(message)
  }

  "Poll Repo" should "show poll's result, when poll is completed" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 16:03:16")),
      dateEnd = Some(dateParser.parse("12:05:30 17:03:16"))) execute PollRepo()
    val (message, _) = Result(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Ok")(message.split("\n")(0))
  }

  "Poll Repo" should "show poll's result, when poll's running with continuous mode on" in {
    val (msg, ctx) = CreatePoll("HI", dateStart = Some(dateParser.parse("12:05:30 16:03:16")),
      dateEnd = Some(dateParser.parse("12:05:30 20:03:16"))) execute PollRepo()
    val (message, _) = Result(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Ok")(message.split("\n")(0))
  }

  "Poll Repo" should "not show poll's result, when poll's running with afterstop mode on" in {
    val (msg, ctx) = CreatePoll("HI", isAfterStop = true, dateStart = Some(dateParser.parse("12:05:30 16:03:16")),
      dateEnd = Some(dateParser.parse("12:05:30 20:03:16"))) execute PollRepo()
    val (message, _) = Result(msg.split(":")(1).trim.toInt) execute ctx
    assertResult("Error: Poll's result isn't available until the end")(message)
  }

  "Poll Repo" should "switch context, when repo exists this id" in {
    val (msg, ctx) = CreatePoll("HI") execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (message, repo) = Begin(id) execute ctx
    assertResult(s"Ok: switch to poll with number $id")(message)
    assertResult(repo.currentContextPoll)(id)
  }

  "Poll Repo" should "not switch context, when repo doesn't exist this id" in {
    val (msg, ctx) = CreatePoll("HI") execute PollRepo()
    val id = msg.split(":")(1).trim.toInt + 1
    val (message, _) = Begin(id) execute ctx
    assertResult(s"Error: This poll doesn't exist")(message)
  }

  "Poll Repo" should " switch off the context, when repo exists this id" in {
    val (msg, ctx1) = CreatePoll("HI") execute PollRepo()
    val id = msg.split(":")(1).trim.toInt
    val (_, ctx2) = Begin(id) execute ctx1
    val (message, repo) = End() execute ctx2
    assertResult(s"Ok: switch off the context")(message)
    assertResult(repo.currentContextPoll)(-1)
  }

  "Poll Repo" should " not switch off the context, when repo isn't in context mode" in {
    val repo = PollRepo()
    assertResult(repo.currentContextPoll)(-1)
    val (message, actualRepo) = End() execute repo
    assertResult(s"Error: The context is already switched off")(message)
    assertResult(actualRepo.currentContextPoll)(-1)
  }
}
