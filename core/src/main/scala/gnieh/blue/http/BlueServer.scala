/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gnieh.blue
package http

import com.typesafe.config.Config

import config._

import scala.concurrent.duration._

import akka.actor.{
  ActorSystem,
  Props
}
import akka.io.IO

import spray.can.Http

import spray.routing.HttpServiceActor

import scala.concurrent.SyncVar

class BlueServer(configuration: Config)(implicit system: ActorSystem) {

  val port =
    configuration.getInt("http.port")

  val host =
    configuration.getString("http.host")

  def start(): Unit = {
    val actor = system.actorOf(Props(new ServerActor(system, configuration)))
    IO(Http) ! Http.Bind(actor, host, port)
  }

  def stop(): Unit =
    IO(Http) ! Http.Unbind(Duration.Inf)

}

private class ServerActor(system: ActorSystem, config: Config) extends HttpServiceActor {

  val apiPrefix =
    pathPrefix(separateOnSlashes(config.getString("blue.api.path-prefix")))

  val routes = {
    val actors = for {
      (path, StringValue(actorClass)) <- config.getConfig("blue.api.routes")
      service = system.actorOf(Props(Class.forName(actorClass)))
    } yield pathPrefixTest(separateOnSlashes(path)) { ctx => service ! ctx }
    if (actors.isEmpty)
      reject
    else if (actors.size == 1)
      actors.head
    else
      actors.reduce(_ ~ _)
  }

  def receive = runRoute(apiPrefix(routes))

}
