package com.rockthejvm.reviewboard.repositories

import zio.test.*
import zio.*
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.syntax.*
import java.sql.SQLException

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec:

  val rtjvm                                             = Company(-1L, "Rock the JVM", "rockthejvm.com", Company.makeSlug("Rock the JVM"))
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("create a company") {
        val program =
          for
            repo    <- ZIO.service[CompanyRepository]
            company <-
              repo.create(rtjvm)
          yield company
        program.assert {
          case Company(id, name, url, slug, _, _, _, _, _) => true
          case null                                        => false
        }
      },
      test("creating a duplicate should error") {
        val program =
          for
            repo <- ZIO.service[CompanyRepository]
            _    <- repo.create(rtjvm)
            err  <- repo.create(rtjvm).flip
          yield err
        program.assert(_.isInstanceOf[SQLException])
      },
      test("get by id and slug") {
        val program = for
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rtjvm)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        yield (fetchedById, fetchedBySlug)
        program.assert {
          case (Some(company), Some(company2)) => company == company2
          case _                               => false
        }
      },
      test("update company") {
        val program = for
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rtjvm)
          updated     <- repo.update(company.id, _.copy(name = "Rock the JVM 2"))
          fetchedById <- repo.getById(company.id)
        yield (updated, fetchedById)
        program.assert {
          case (updated, Some(fetched)) => updated == fetched
          case _                        => false
        }
      },
      test("delete company") {
        val program = for
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rtjvm)
          _       <- repo.delete(company.id)
          fetched <- repo.getById(company.id)
        yield fetched
        program.assert(_.isEmpty)
      },
      test("get all companies") {
        val program = for
          repo     <- ZIO.service[CompanyRepository]
          company1 <- repo.create(rtjvm)
          company2 <- repo.create(rtjvm.copy(name="updated name", slug="updated-name", url="updated-url.com"))
          all      <- repo.getAll
        yield all
        program.assert(_.size == 2)
      }
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
