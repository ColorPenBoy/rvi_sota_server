/**
 * Copyright: Copyright (C) 2015, Jaguar Land Rover
 * License: MPL-2.0
 */
package org.genivi.sota.resolver.components

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.stream.ActorMaterializer
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Regex
import io.circe.generic.auto._
import org.genivi.sota.data.Namespace._
import org.genivi.sota.marshalling.CirceMarshallingSupport._
import org.genivi.sota.marshalling.RefinedMarshallingSupport._
import org.genivi.sota.resolver.common.RefinementDirectives.refinedPartNumber
import org.genivi.sota.resolver.common.Errors
import scala.concurrent.ExecutionContext
import slick.driver.MySQLDriver.api._
import Directives._

/**
 * API routes for creating, deleting, and listing components.
 * @see {@linktourl http://pdxostc.github.io/rvi_sota_server/dev/api.html}
 */
class ComponentDirectives(namespaceExtractor: Directive1[Namespace])
                         (implicit system: ActorSystem,
                          db: Database,
                          mat: ActorMaterializer,
                          ec: ExecutionContext) {

  def searchComponent(ns: Namespace): Route =
    parameter('regex.as[String Refined Regex].?) { re =>
      val query = re.fold(ComponentRepository.list)(re => ComponentRepository.searchByRegex(ns, re))
      complete(db.run(query))
    }

  def addComponent(ns: Namespace, part: Component.PartNumber): Route =
    entity(as[Component.DescriptionWrapper]) { descr =>
      val comp = Component(ns, part, descr.description)
      complete(db.run(ComponentRepository.addComponent(comp)).map(_ => comp))
    }


  def deleteComponent(ns: Namespace, part: Component.PartNumber): Route =
    completeOrRecoverWith(ComponentRepository.removeComponent(ns, part)) {
      Errors.onComponentInstalled
    }

  /**
   * API route for components.
   * @return      Route object containing routes for creating, editing, and listing components
   * @throws      Errors.ComponentIsInstalledException on DELETE call, if component doesn't exist
   */
  def route: Route =
    (pathPrefix("components") & namespaceExtractor) { ns =>
      (get & pathEnd) {
        searchComponent(ns)
      } ~
      (put & refinedPartNumber & pathEnd) { part =>
        addComponent(ns, part)
      } ~
      (delete & refinedPartNumber & pathEnd) { part =>
        deleteComponent(ns, part)
      }
    }

}
