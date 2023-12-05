package com.rockthejvm.reviewboard.repositories

import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource
import zio.*
import zio.test.*

trait RepositorySpec:
  // test containers

  // spawn a Postgres instance on Docker just for this test
  private def createContainer() =
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript("sql/companies.sql")
    container.start()
    container
    // create a Datasource to connect to the Postgres instance

  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource =
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl())
    dataSource.setUser(container.getUsername())
    dataSource.setPassword(container.getPassword())
    dataSource
    // use the Datasource (a ZLayer) to create a Quill context (as a ZLayer)

  val dataSourceLayer = ZLayer {
    for
      container  <- ZIO.acquireRelease(ZIO.attempt(createContainer()))(container =>
                      ZIO.attempt(container.stop()).ignoreLogged
                    )
      dataSource <- ZIO.attempt(createDataSource(container))
    yield dataSource
  }
