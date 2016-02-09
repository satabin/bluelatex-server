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

import akka.actor.{
  Actor,
  Status
}

import scala.io.Codec

import java.io.{
  IOException,
  FileNotFoundException
}
import java.nio.charset.CodingErrorAction

import better.files._
import Cmds._

import com.typesafe.config.ConfigFactory

import config._

import scala.annotation.tailrec

object FsStore {

  val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

}

/** Represents a file system tree and synchronizes string content with it.
 *  For the moment it assumes that the string files are not modified by someone else.
 *  This could be done by using file reconciliation algorithm in the future.
 *
 *  @author Lucas Satabin
 */
class FsTree(private val file: File) {

  import scala.collection.mutable.Map

  import FsStore._

  private val _children =
    Map.empty[String, FsTree]
  private var _content: Option[String] =
    None
  private var _lastModified: Long =
    -1
  private var _deleted =
    false

  def keys(): PathTree =
    ???

  def save(): Unit =
    if (_deleted) {
      // this node was deleted, then remove from FS (with potential children in case of directory)
      file.delete()
    } else _content match {
      case Some(c) =>
        // let's save it if needed
        if (_lastModified > file.lastModifiedTime.toEpochMilli)
          file.overwrite(c)(codec)
      case None =>
        // save the children and clear deleted ones
        for ((f, child) <- _children) {
          child.save()
          if (child.deleted) {
            _children -= f
          }
        }
    }

  def load(): Unit =
    if (file.isDirectory) {
      _deleted = false
      _lastModified = file.lastModifiedTime.toEpochMilli
      _content = None
      for ((f, child) <- _children) {
        if (child.deleted) {
          _children -= f
        } else {
          child.load()
        }
      }
    } else if (file.exists) {
      _deleted = false
      _lastModified = file.lastModifiedTime.toEpochMilli
      _children.clear()
      _content = Option(file.contentAsString(codec))
    }

  def deleted =
    _deleted

  def update(key: List[String], content: String): Unit = key match {
    case f :: rest =>
      val child = _children.getOrElseUpdate(f, new FsTree(file / f))
      child(rest) = content
      if (_deleted) {
        _deleted = false
        _lastModified = System.currentTimeMillis
      }
    case Nil =>
      _content = Option(content)
      _lastModified = System.currentTimeMillis
      if (_deleted)
        _deleted = false
  }

  def remove(key: List[String]): Unit = key match {
    case f :: rest =>
      _children.get(f).foreach(_.remove(rest))
    case Nil =>
      // clear content and mark as deleted
      _content = None
      _lastModified = -1
      _children.clear()
      _deleted = true
  }

  def get(key: List[String]): Option[String] =
    if (_deleted)
      None
    else
      key match {
        case f :: rest =>
          _children.get(f).flatMap(_.get(rest))
        case Nil =>
          if (_content.isEmpty && file.exists && !file.isDirectory) {
            // the file exists on FS but was not loaded, just do it
            load()
          }
          _content
      }
}

class FsStore(paperId: String) extends Actor {

  val conf = ConfigFactory.load()

  val persistenceDir =
    conf.as[File]("bluelatex.persistence.fs.directory")

  val base = persistenceDir / paperId

  private var files = new FsTree(base)

  override def postStop(): Unit = {
    super.postStop()
    // save one last time
    files.save()
  }

  def receive = {

    case Save(file, data) =>
      try {
        files(file) = data
        sender ! Saved(file)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case SaveFile(path, origin) =>
      try {
        origin.moveTo(base / path.mkString("/"))
        sender ! Saved(path)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Remove(file) =>
      try {
        files.remove(file)
        sender ! Removed(file)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Read(file) =>
      try {
        val c = files.get(file)
        sender ! Content(file, c)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Commit =>
      try {
        val c = files.save()
        sender ! Committed
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case ListAll =>
      try {
        val k = files.keys()
        sender ! AllKeys(k)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

  }

}
