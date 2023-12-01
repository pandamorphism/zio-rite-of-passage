package com.rockthejvm.reviewboard.controllers

import zio.*
import zio.test.*
import zio.json.*
import com.rockthejvm.reviewboard.http.controllers.CompanyController
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import sttp.client3.*
import sttp.tapir.generic.auto.*
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.domain.data.Company
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.services.CompanyService
import collection.mutable
import scala.meta.internal.javacp.BaseType.Z
object CompanyControllerSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]
  private val rtjvm                     = Company(1, "rock-the-jvm", "Rock the JVM", "https://rockthejvm.com")
  private val serviceStub               = new CompanyService {

    override def create(request: CreateCompanyRequest): Task[Company] = ZIO.succeed {
      rtjvm
    }
    override def getAll: Task[List[Company]]                          = ZIO.succeed(List(rtjvm))
    override def getById(id: Long): Task[Option[Company]]             = ZIO.succeed(
      if (id == 1) Some(rtjvm) else None
    )
    override def getBySlug(slug: String): Task[Option[Company]]       = ZIO.succeed(
      if (slug == "rock-the-jvm") Some(rtjvm) else None
    )
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) = for {
    controller  <- CompanyController.makeZIO
    backendStub <- ZIO.succeed(
                     TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
                       .whenServerEndpointRunLogic(endpointFun(controller))
                       .backend()
                   )

  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] = suite("CompanyControllerSpec")(
    test("create a company") {
      val program = for {
        backendStub <- backendStubZIO(_.create)
        response    <- basicRequest
                         .post(uri"/companies")
                         .body(CreateCompanyRequest("Rock the JVM", "https://rockthejvm.com").toJson)
                         .send(backendStub)

      } yield response.body
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(rtjvm)
      }
    },
    test("get all") {
      val program = for {
        backendStub <- backendStubZIO(_.getAll)
        response    <- basicRequest
                         .get(uri"/companies")
                         .send(backendStub)

      } yield response.body
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[List[Company]].toOption)
          .contains(
            List(rtjvm)
          )
      }
    },
    test("get by id") {
      val program = for {
        backendStub <- backendStubZIO(_.getById)
        response    <- basicRequest
                         .get(uri"/companies/1")
                         .send(backendStub)

      } yield response.body
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(
            rtjvm
          )
      }
    }
  ).provide(
    ZLayer.succeed(serviceStub)
  )
}
