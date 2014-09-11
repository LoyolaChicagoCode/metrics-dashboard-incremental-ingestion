package controllers

import java.util.concurrent.TimeUnit

import com.mongodb.WriteResult
import com.mongodb.casbah.commons.MongoDBObject
import org.eclipse.egit.github.core.Repository
import org.eclipse.egit.github.core.service.RepositoryService
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import global.Global.database
import global.Global.githubClient

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Try, Success, Failure}

case class RepoNotFoundException(msg: String) extends Exception(msg)
case class AlreadyExistsException(msg: String) extends Exception(msg)

object Repositories extends Controller {

  /*
  def getAll = Action {
    request =>
  }

  def getOne(id: Long) = Action {
    request =>
  }
  */

  import play.api.libs.json._


  implicit val rds = (__ \ 'repo).read[String]

  def addRepository() = Action(parse.tolerantJson) {
    request => request.body.validate[String].map {
      case repoName =>
        try {
          addRepoToDB(repoName)
          Created(s"Repository $repoName added to watch list")
        } catch {
          case rnfe: RepoNotFoundException => BadRequest(rnfe.getMessage)
          case aee: AlreadyExistsException => BadRequest(aee.getMessage)
          case iae: IllegalArgumentException => BadRequest(iae.getMessage)
          case e: Exception => BadRequest(e.getMessage)
        }
    }.recoverTotal {
      e => BadRequest("Invalid Json. Expecting json of the form {\"repo\": \"user/repo\"}")
    }
  }

  private def addRepoToDB(repo: String) = {

    val repoDocument = MongoDBObject("repo" -> repo)
    val splitRepo = repo.split("/")
    if (splitRepo.size != 2)
      throw new IllegalArgumentException("Repository must have the form user/repo. Example: mdotson/metrics-dashboard")

    val repoService = new RepositoryService(githubClient)

    // get repo info premptively in case it's not in our DB
    val githubRepoInfFut: Future[Try[Repository]] = future {
      Try(repoService.getRepository(splitRepo.head, splitRepo.last))
    }

    val repoCollectionFut = future {
      database.getCollection("repositories")
    }

    val dbRepoInfoFut = for {
      repoCollection <- repoCollectionFut
    } yield Option(repoCollection.findOne(repoDocument))


    val dbRepoInfo = Await.result(dbRepoInfoFut, Duration(1, TimeUnit.SECONDS))

    dbRepoInfo match {
      case Some(_) => throw new AlreadyExistsException(s"Repository $repo is already being watched.")
      case None =>
        val githubRepoInfo = Await.result(githubRepoInfFut, Duration(1, TimeUnit.SECONDS))
        githubRepoInfo match {
          case Success(s) => for {
            repoCollection <- repoCollectionFut
          } yield repoCollection.insert(repoDocument)
          case Failure(f) => throw new RepoNotFoundException(s"Repository $repo does not exist on GitHub.")
        }
    }
  }
}
