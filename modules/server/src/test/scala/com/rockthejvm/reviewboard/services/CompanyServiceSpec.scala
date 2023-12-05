package com.rockthejvm.reviewboard.services

import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import collection.mutable
import com.rockthejvm.reviewboard.domain.data.Company

object CompanyServiceSpec extends ZIOSpecDefault:

  val service       = ZIO.serviceWithZIO[CompanyService]
  val stubRepoLayer = ZLayer.succeed(new CompanyRepository {
    val companies                                = mutable.Map.empty[Long, Company]
    override def delete(id: Long): Task[Company] =
      ZIO.attempt(companies.remove(id).get)

    override def update(id: Long, op: Company => Company): Task[Company] =
      ZIO.attempt {
        val current = companies(id)
        val updated = op(current)
        companies.put(id, updated)
        updated
      }

    override def create(company: Company): Task[Company]        = ZIO.succeed {
      val id            = companies.size.toLong + 1
      val companyWithId = company.copy(id = id)
      companies.put(id, companyWithId)
      companyWithId
    }
    override def getAll: Task[List[Company]]                    = ZIO.succeed(companies.values.toList)
    override def getById(id: Long): Task[Option[Company]]       = ZIO.succeed(companies.get(id))
    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed(companies.values.find(_.slug == slug))
  })

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyServiceSpec")(
      test("create a company") {
        val companyZIO = service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
        companyZIO.assert { company =>
          company.name == "Rock the JVM" && company.url == "rockthejvm.com" && company.slug == "rock-the-jvm"
        }
      },
      test("get by id") {
        val program = for
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getById(company.id))
        yield (company, companyOpt)
        program.assert {
          case (company, Some(companyResult)) =>
            companyResult.name == "Rock the JVM" && companyResult.url == "rockthejvm.com" && companyResult.slug == "rock-the-jvm" && company == companyResult
          case _                              => false
        }
      },
      test("get by slug") {
        val program = for
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        yield (company, companyOpt)
        program.assert {
          case (company, Some(companyResult)) =>
            companyResult.name == "Rock the JVM" && companyResult.url == "rockthejvm.com" && companyResult.slug == "rock-the-jvm" && company == companyResult
          case _                              => false
        }
      },
      test("get all") {
        val program = for
          company1 <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          company2 <-
            service(_.create(CreateCompanyRequest("Scala Exercises", "scala-exercises.com")))
          all      <- service(_.getAll)
        yield (company1, company2, all)
        program.assert { case (company1, company2, all) =>
          all.size == 2 && all.contains(company1) && all.contains(company2)
        }
      }
    ).provide(CompanyServiceLive.layer, stubRepoLayer)
