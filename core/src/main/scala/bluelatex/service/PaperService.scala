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
package bluelatex
package service

import persistence._

import synchro._

import scala.concurrent.duration._

import java.util.UUID

import akka.pattern.ask

import spray.json._

import better.files._
import Cmds._

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.directives.ContentTypeResolver

trait PaperService {
  this: CoreService =>

  implicit val timeout = akka.util.Timeout(2.seconds)

  def paperRoute =
    pathPrefix("papers") {
      pathEndOrSingleSlash {
        post {
          val newId = UUID.randomUUID.toString
          onSuccess(synchronizer ? (newId -> CreatePaper)) { _ =>
            complete(JsString(newId))
          }
        }
      } ~
        pathPrefix(Segment) { paperId =>
          pathEndOrSingleSlash {
            delete {
              onSuccess(synchronizer ? (paperId -> DeletePaper)) { _ =>
                complete(JsBoolean(true))
              }
            }
          } ~
            pathPrefix("files") {
              pathEndOrSingleSlash {
                get {
                  onSuccess((synchronizer ? (paperId -> ListAll)).mapTo[PathTree]) { paths =>
                    complete(paths.toJson)
                  }
                }
              } ~
                get {
                  path(Segments) { path =>
                    onSuccess((synchronizer ? (paperId -> Raw)).mapTo[RawContent]) {
                      case RawContent(file, Some(content)) =>
                        complete(HttpEntity(ContentTypeResolver.Default(file.last), content))
                      case RawContent(file, None) =>
                        reject
                    }
                  }
                } ~
                post {
                  path(Segments) { path =>
                    uploadedFile("file") {
                      case (metadata, file) =>
                        onSuccess(synchronizer ? (paperId -> SaveFile(path, file.toScala))) { _ =>
                          complete(JsBoolean(true))
                        }
                    } ~
                      complete(JsBoolean(true))
                  }
                } ~
                delete {
                  path(Segments) { path =>
                    onSuccess(synchronizer ? (paperId -> DeleteDoc(path))) { _ =>
                      complete(JsBoolean(true))
                    }
                  }
                }
            } ~
            path("synchronization") {
              post {
                entity(as[SynchronizationMessage]) { msg =>
                  complete("toto")
                }
              }
            }
        }
    }

}
