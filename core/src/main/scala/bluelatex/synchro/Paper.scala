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
package synchro

import persistence._

import scala.collection.mutable.Map
import akka.actor.{
  Actor,
  ActorRef,
  Props,
  Status,
  Stash,
  Terminated
}
import akka.event.Logging

import java.util.{
  Date,
  Calendar
}

import name.fraser.neil.plaintext.DiffMatchPatch

import java.io.FileNotFoundException

/** This actor handles synchronisation of documents.
 *  It represents the files of a given document.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
class Paper(
  paperId: PaperId,
  storeProps: Props,
  dmp: DiffMatchPatch)
    extends Actor with Stash {

  private var store: ActorRef =
    null

  private val messageBus =
    new MessageEventBus

  private val peers = Map.empty[String, ActorRef]

  private def getPeerRef(peerId: String): ActorRef =
    peers.get(peerId) match {
      case Some(ref) =>
        ref
      case None =>
        // create the actor
        val act = context.actorOf(Props(classOf[Peer], paperId, peerId, messageBus), peerId)
        context.watch(act)
        peers(peerId) = act
        act
    }

  val logger = Logging(context.system, this)

  override def preStart(): Unit = {
    // instantiate the store
    store = context.actorOf(storeProps)
  }

  def receive = started(Calendar.getInstance().getTime())

  def started(lastModificationTime: Date): Receive = {

    case r: persistence.Request =>
      store.forward(r)

    case DeletePaper =>
      // remove the root node
      store ! Remove(Nil)
      // stop this actor
      context.stop(self)

    case Status.Failure(e) =>
      // log the error
      logger.error(f"An error occurred when persisting paper $paperId", e)

    case session @ SynchronizationMessage(peerId, _, _) =>
      // forward to the peer actor
      getPeerRef(peerId).forward(session)

    case GetDoc(path) =>
      // get or create document
      store.forward(Read(path))

    case DeleteDoc(path) =>
      store.forward(Remove(path))

    case UpdateDoc(path, content) =>
      store ! Save(path, content)
      context.become(started(Calendar.getInstance.getTime))

    case PersistPaper =>
      store ! Commit

    case GetLastModificationDate =>
      sender ! lastModificationTime

    case Terminated(peer) =>
      peers -= peer.path.name
      // if all peers terminated, then terminate this paper representation
      if (peers.isEmpty) {
        context.stop(self)
      }

  }

}
