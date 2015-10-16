/**
  * Copyright: Copyright (C) 2015, Jaguar Land Rover
  * License: MPL-2.0
  */
package org.genivi.sota.refined

object implicits {
  import cats.{Eq, Show}
  import eu.timepit.refined.Refined

  implicit def refinedEq[T, P](implicit ev: Eq[T]) : Eq[T Refined P] = Eq.instance((a, b) => ev.eqv( a.get, b.get))

  implicit def refinedShow[T, P](implicit ev: Show[T]) : Show[T Refined P] = Show.show( x => ev.show(x.get) )

}