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
package service

import akka.actor.Actor
import spray.routing.HttpServiceActor

class UsersService extends HttpServiceActor {

  def receive = runRoute {
    pathPrefix("users") {
      pathEndOrSingleSlash {
        post {
          // registers a new user
          registerUser
        } ~
          get {
            // gets the list of users matching the given pattern
            getUsers
          }
      } ~
        pathPrefix(Segment) { username =>
          pathEndOrSingleSlash {
            delete {
              // unregisters the authenticated user
              deleteUser(username)
            }
          } ~
            path("info") {
              get {
                // gets the data of the given user
                getUserInfo(username)
              } ~
                patch {
                  // save the data for the authenticated user
                  modifyUser(username)
                }
            } ~
            path("papers") {
              get {
                // gets the list of papers the given user is involved in
                getUserPapers(username)
              }
            } ~
            path("reset") {
              post {
                // performs password reset
                resetUserPassword(username)
              } ~
                get {
                  // generates a password reset token
                  generatePasswordReset(username)
                }
            }
        }
    }
  }
}
