package com.rockthejvm

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???

// repository
trait JobRepository:
  def getById(id: Long): ZIO[Any, Throwable, Option[Job]]
  def create(job: Job): ZIO[Any, Throwable, Job]
  def update(id: Long, op: Job => Job): ZIO[Any, Throwable, Job]
  def delete(id: Long): ZIO[Any, Throwable, Job]
  def get: ZIO[Any, Throwable, List[Job]]

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase])
    extends JobRepository: // some methods e.g. run a query
  import quill.*
  // step 2 - schemas for create, update...

  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs") // specify the table name
  inline given instMeta: InsertMeta[Job] =
    insertMeta[Job](_.id) // columns to be excluded from insert
  inline given upMeta: UpdateMeta[Job] =
    updateMeta[Job](_.id) // columns to be excluded from update

  override def create(job: Job): ZIO[Any, Throwable, Job] =
    run {
      query[Job]
        .insertValue(lift(job))
        .returning(j => j)
    }

  override def update(id: Long, op: Job => Job): ZIO[Any, Throwable, Job] =
    for
      current <- getById(id).someOrFail(new RuntimeException(s"Job with id $id not found"))
      updated <- run {
        query[Job]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(j => j)
      }
    yield updated

  override def getById(id: Long): ZIO[Any, Throwable, Option[Job]] =
    run {
      query[Job]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def delete(id: Long): ZIO[Any, Throwable, Job] =
    run {
      query[Job]
        .filter(_.id == lift(id))
        .delete
        .returning(j => j)
    }

  override def get: ZIO[Any, Throwable, List[Job]] =
    run {
      query[Job]
    }
end JobRepositoryLive

object JobRepositoryLive:
  val layer = ZLayer.fromFunction(new JobRepositoryLive(_))
end JobRepositoryLive

object QuillDemo extends ZIOAppDefault:
  val program =
    for
      repo <- ZIO.service[JobRepository]
      _    <- repo.create(Job(1, "Scala developer", "London", "Rock the JVM"))
      _    <- repo.create(Job(2, "Java developer", "London", "Rock the JVM"))
      _    <- repo.create(Job(3, "FP developer", "New York", "Google"))
    yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
    .provide(
      JobRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix(
        "mydbconf"
      ) // reads the config section in application.conf and spins up a datasource
    )
    .debug("Results")
end QuillDemo
