/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.core

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.Marshaller._
import akka.http.scaladsl.server.PathMatchers.Slash
import akka.http.scaladsl.server.{Directive1, Directives}
import akka.stream.ActorMaterializer
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import io.circe.generic.auto._
import io.circe.syntax._
import org.genivi.sota.core.common.NamespaceDirective._
import org.genivi.sota.core.data._
import org.genivi.sota.core.db.UpdateSpecs
import org.genivi.sota.core.resolver.ExternalResolverClient
import org.genivi.sota.data.Namespace._
import org.genivi.sota.data.{PackageId, Vehicle}
import org.genivi.sota.marshalling.CirceMarshallingSupport
import org.genivi.sota.rest.Validation._
import slick.driver.MySQLDriver.api.Database

class UpdateRequestsResource(db: Database, resolver: ExternalResolverClient, updateService: UpdateService)
                            (implicit system: ActorSystem, mat: ActorMaterializer) {

  import CirceMarshallingSupport._
  import Directives._
  import UpdateSpec._
  import WebService._
  import eu.timepit.refined.string.uuidValidate
  import system.dispatcher

  implicit val _db = db

  def fetch(uuid: Refined[String, Uuid]) = {
    complete(db.run(UpdateSpecs.listUpdatesById(uuid)))
  }

  def queueVehicleUpdate(ns: Namespace, vin: Vehicle.Vin) = {
    entity(as[PackageId]) { packageId =>
      val result = updateService.queueVehicleUpdate(ns, vin, packageId)
      complete(result)
    }
  }

  def fetchUpdates = {
    complete(updateService.all(db, system.dispatcher))
  }

  def createUpdate(ns: Namespace) = {
    entity(as[UpdateRequest]) { req =>
      complete(
        updateService.queueUpdate(
          req,
          pkg => resolver.resolve(ns, pkg.id).map {
            m => m.map { case (v, p) => (v.vin, p) }
          }
        )
      )
    }
  }

  val route = pathPrefix("updates") {
    (get & extractUuid & pathEnd) {
      fetch
    } ~
    (extractNamespace & extractVin & post) { (ns, vin) =>
      queueVehicleUpdate(ns, vin)
    } ~
    pathEnd {
      get {
        fetchUpdates
      } ~
      (post & extractNamespace) { ns =>
        createUpdate(ns)
      }
    }
  }
}