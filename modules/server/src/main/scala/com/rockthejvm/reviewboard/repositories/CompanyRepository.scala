package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait CompanyRepository:
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository:
  import quill.*
  inline given schema: SchemaMeta[Company] =
    schemaMeta[Company]("companies") // specify the table name
  inline given instMeta: InsertMeta[Company] =
    insertMeta[Company](_.id) // columns to be excluded from insert
  inline given upMeta: UpdateMeta[Company] =
    updateMeta[Company](_.id) // columns to be excluded from update

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company]
        .filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(c => c)
    }

  override def getAll: Task[List[Company]] =
    run {
      query[Company]
    }

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(c => c)
    }

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Company with id $id not found"))
      updated <- run {
                   query[Company]
                     .filter(_.id == lift(id))
                     .updateValue(lift(op(current)))
                     .returning(c => c)
                 }
    } yield updated

object CompanyRepositoryLive:
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(new CompanyRepositoryLive(_))
  }

object CompanyRepositoryDemo extends ZIOAppDefault:
  val program = for
    repo    <- ZIO.service[CompanyRepository]
    created <-
      repo.create(
        Company(id = -1, name = "Rock the JVM", url = "rockthejvm.com", slug = "rock-the-jvm")
      )
  yield created
 
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] = program.debug.provide(
    CompanyRepositoryLive.layer,
    Quill.Postgres.fromNamingStrategy(SnakeCase),
    Quill.DataSource.fromPrefix("rockthejvm.db")
  )
