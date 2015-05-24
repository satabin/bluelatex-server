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
package couch

import spray.json._

import gnieh.sohva._

import permission._

trait BlueProtocol extends SohvaProtocol {

  implicit val paperFormat = couchFormat[Paper]

  implicit object PermissionFormat extends JsonFormat[Permission] {
    def read(json: JsValue) = json match {
      case JsString(s) => Permission(s)
      case _           => deserializationError("Permission string expected")
    }
    def write(p: Permission) =
      JsString(p.name)
  }

  implicit object RoleFormat extends JsonFormat[Role] {
    def read(json: JsValue) = json match {
      case JsString(s) =>
        s match {
          case "author"    => Author
          case "reviewer"  => Reviewer
          case "other"     => Other
          case "guest"     => Guest
          case "anonymous" => Anonymous
          case s           => deserializationError(f"Unknown role $s")
        }
      case _ =>
        deserializationError("Role string expected")
    }
    def write(r: Role) =
      JsString(r.name)
  }

  implicit val phaseFormat = couchFormat[Phase]

  implicit val paperPhaseFormat = couchFormat[PaperPhase]

  implicit val userRoleFormat = jsonFormat2(UserRole)

  implicit val userFormat = couchFormat[User]

  implicit val usersGroupsFormat = jsonFormat2(UsersGroups)

  implicit val paperRoleFormat = couchFormat[PaperRole]

}
