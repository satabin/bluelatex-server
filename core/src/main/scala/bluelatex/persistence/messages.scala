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
package persistence

import better.files.File

/** A store must take messages of this type into account. */
sealed trait Request

case class Save(path: Path, content: String) extends Request

case class SaveFile(path: Path, file: File) extends Request

case class Remove(path: Path) extends Request

case class Read(path: Path) extends Request

case object Commit extends Request

case object ListAll extends Request

/** A store must answer with messages of this type or an error. */
sealed trait Response

case class Saved(path: Path) extends Response

case class Removed(path: Path) extends Response

case class Content(path: Path, content: Option[String]) extends Response

case object Committed extends Response

case class AllKeys(keys: PathTree) extends Response
