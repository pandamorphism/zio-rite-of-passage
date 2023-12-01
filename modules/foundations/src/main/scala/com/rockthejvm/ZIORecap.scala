package com.rockthejvm

import zio.*

object ZIORecap extends ZIOAppDefault:
  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing]   = ZIO.fail("Meaning of life is unknown")
  // suspension/delay
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  // map/flatMap
  val improvedMeaningOfLife = meaningOfLife.map(_ + 1)
  val printingMeaningOfLife =
    meaningOfLife.flatMap(n => Console.printLine(s"the meaning of life is $n"))
  val smallProgram =
    for
      _       <- Console.printLine("What's your name?")
      name <- Console.readLine
      _    <- Console.printLine(s"Hello, $name!")
    yield ()

  // error handling
  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("This is a side effect")
    val string: String = null
    string.length
  }
  val catchError = anAttempt.catchAll(_ => ZIO.succeed("Returning a default value"))
  val catchSelective = anAttempt.catchSome { case _: NullPointerException =>
    ZIO.succeed(-1)
  }

  // fibers
  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair =
    for
      a <- delayedValue
      b <- delayedValue
    yield (a, b) // this takes 2 seconds

  val aPairPar =
    for
      fiberA <- delayedValue.fork
      fiberB <- delayedValue.fork
      a      <- fiberA.join
      b      <- fiberB.join
    yield (a, b) // this takes 1 second

  val interruptedFiber =
    for
      fib <- delayedValue.onInterrupt(ZIO.succeed(println("I have been interrupted"))).fork
      _   <- ZIO.succeed(println("cancelling fiber")).delay(500.millis) *> fib.interrupt
      _   <- fib.join
    yield ()

  val ignoredInterruption =
    for
      fib <- ZIO
        .uninterruptible(
          delayedValue
            .onInterrupt(ZIO.succeed(println("I have been interrupted")))
            .flatMap(v => Console.printLine(v))
        )
        .fork
      _ <- ZIO.succeed(println("cancelling fiber")).delay(500.millis) *> fib.interrupt
      f <- fib.join
    yield f

  // many APIs
  val aPairPar_v3 = delayedValue.zipPar(delayedValue)
  val randomX10   = ZIO.foreachPar(1 to 10)(_ => delayedValue)

  val program =
    for
      _ <- subscribe(User("Daniel", "daniel@rockthejvm.com"))
      _ <- subscribe(User("John", "jon@rockthejvm.com"))
    yield ()

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] =
    for
      sub <- ZIO.service[UserSubscription]
      _   <- sub.subscribe(user)
    yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      ConnectionPool.live(10),
      UserDatabase.live,
      EmailService.live,
      UserSubscription.live
    )

  // dependencies
  case class User(name: String, email: String)

  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase):
    def subscribe(user: User): Task[Unit] =
      for
        _ <- emailService.email(user)
        _ <- userDatabase.insert(user)
        _ <- Console.printLine(s"User $user subscribed")
      yield ()

  class EmailService:
    def email(user: User): Task[Unit] = ZIO.succeed(s"emailed $user")

  class UserDatabase(connection: ConnectionPool):
    def insert(user: User): Task[Unit] = ZIO.succeed(s"inserted $user")

  class ConnectionPool(nConnections: Int):
    def get: Task[Connection] = ZIO.succeed(Connection())

  case class Connection()

  object UserSubscription:
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction(new UserSubscription(_, _))

  object EmailService:
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService)

  object UserDatabase:
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))

  object ConnectionPool:
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(new ConnectionPool(nConnections))
