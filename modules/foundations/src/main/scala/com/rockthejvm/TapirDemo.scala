package com.rockthejvm

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server
import zio.json.{DeriveJsonCodec, JsonCodec}

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault:
  val simplestEndpoint = endpoint
    .tag("simplest")
    .name("simplest")
    .description("the simplest endpoint in the world")
    .get
    .in("api" / "simplest")
    .out(plainBody[String])
    .serverLogicSuccess[Task](_ => ZIO.succeed("Hello, HTTP!"))

// create
  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create job")
    .description("create a job posting")
    .post
    .in("api" / "jobs")
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess { request =>
      val job = Job(
        id = db.keys.max + 1,
        title = request.title,
        url = request.url,
        company = request.company
      )
      db.put(job.id, job)
      ZIO.succeed(job)
    }
// get by id
  val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("get job by id")
    .description("get a job posting by id")
    .get
    .in("api" / "jobs" / path[Long]("jobId"))
    .out(jsonBody[Job])
    .serverLogicSuccess { id =>
      ZIO.fromOption(db.get(id)).orElseFail(new NoSuchElementException(s"Job $id not found"))
    }
// get all
  val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("get all jobs")
    .description("get all jobs from the database")
    .get
    .in("api" / "jobs")
    .out(jsonBody[List[Job]])
    .serverLogicSuccess { _ =>
      ZIO.succeed(db.values.toList)
    }

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS
    ).toHttp(List(createEndpoint, getByIdEndpoint, getAllEndpoint))
  )

  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS
    ).toHttp(simplestEndpoint)
  )

  // simulate a job board
  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1, "Scala developer", "https://rockthejvm.com/jobs/scala", "Rock the JVM"),
    2L -> Job(2, "Java developer", "https://rockthejvm.com/jobs/java", "Rock the JVM"),
    3L -> Job(3, "FP developer", "https://rockthejvm.com/jobs/fp", "Rock the JVM")
  )

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = serverProgram.provide(
    Server.default
  )