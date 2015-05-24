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
package gnieh

import com.typesafe.config.Config

import java.io.File

import java.util.Properties

import scala.collection.JavaConverters._

import scala.concurrent.Future

import gnieh.sohva.async.{
  CouchClient,
  Session
}

import java.util.concurrent.TimeUnit

package object blue {

  type UserInfo = gnieh.sohva.UserInfo

  implicit class MultiMap[Key, Value](val map: Map[Key, Set[Value]]) extends AnyVal {

    def addBinding(key: Key, value: Value): Map[Key, Set[Value]] = map.get(key) match {
      case Some(set) =>
        // add the value to the set
        map.updated(key, set + value)
      case None =>
        // create the binding
        map.updated(key, Set(value))
    }

  }

  implicit class PaperConfiguration(val config: Config) extends AnyVal {

    import FileUtils._

    def paperDir(paperId: String): File =
      new File(config.getString("blue.paper.directory")) / paperId

    def paperFile(paperId: String): File =
      paperDir(paperId) / "main.tex"

    def resource(paperId: String, resourceName: String): File =
      paperDir(paperId) / resourceName

    def bibFile(paperId: String): File =
      paperDir(paperId) / "references.bib"

    def clsDir: File =
      new File(config.getString("blue.paper.classes"))

    def clsFiles: List[File] =
      clsDir.filter(_.getName.endsWith(".cls"))

    def cls(name: String): File =
      clsDir / s"$name.cls"

  }

  implicit class BlueConfiguration(val config: Config) extends AnyVal {

    def recaptchaPrivateKey =
      if (config.hasPath("recaptcha.private-key"))
        Some(config.getString("recaptcha.private-key"))
      else
        None

    def emailConf = {
      val props = new Properties
      for (entry <- config.getConfig("mail").entrySet.asScala) {
        props.setProperty("mail." + entry.getKey, entry.getValue.unwrapped.toString)
      }
      props
    }

    def templateDir =
      new File(config.getString("blue.template.directory"))

  }

  /** The couchdb configuration part */
  implicit class CouchConfiguration(val config: Config) extends AnyVal {

    def couchAdminName = config.getString("couch.admin-name")

    def couchAdminPassword = config.getString("couch.admin-password")

    def couchDesigns = new File(config.getString("couch.design.dir"))

    def adminSession(client: CouchClient) =
      client.startBasicSession(couchAdminName, couchAdminPassword)

    def asAdmin[T](client: CouchClient)(code: Session => Future[T]) =
      code(adminSession(client))

    def designDir(dbName: String) =
      new File(couchDesigns, dbName)

    def database(key: String) = {
      val k = s"couch.database.$key"
      if (config.hasPath(k))
        config.getString(k)
      else
        key
    }

    /** The list of \BlueLaTeX databases */
    def databases =
      config.getObject("couch.database").asScala.keys.toList

    /** The list of default roles to assign to a newly created user */
    def defaultRoles =
      config.getList("couch.user.roles").unwrapped.asScala.map(_.toString).toList

    /** The reset password token validity in seconds */
    def tokenValidity =
      config.getDuration("couch.user.token-validity", TimeUnit.SECONDS).toInt

  }

}
