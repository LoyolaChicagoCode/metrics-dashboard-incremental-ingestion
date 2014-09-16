package controllers

import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import scala.concurrent._
import scala.util.{Try, Success, Failure}
import com.novus.salat._
import model.Repository
import globalobj.Global.ctx
import play.api.libs.json._
import globalobj.RemoteConnections._

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

  // used for parsing the POST
  implicit val rds = (__ \ 'repo).read[String]

  /**
   * Tries to add the repository from the JSON body to the database of watched repos
   * @return
   */
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

  /**
   * Add the repository to the database if it's valid
   *
   * @param repo repository user passed in
   * @return
   */
  private def addRepoToDB(repo: String) = {

    val repoPair = validateAndPairRepo(repo)

    // get repo info premptively in case it's not in our DB
    val githubRepoInfoFut = future {
      Try(repositoryService.getRepository(repoPair._1, repoPair._2))
    }

    val repoDocument = MongoDBObject("full_name" -> repo)

    // check if the repo is in the DB already
    val dbRepoInfoFut = future {
      repositoriesCollection.findOne(repoDocument)
    }

    dbRepoInfoFut map {
      case Some(_) => throw new AlreadyExistsException(s"Repository $repo is already being watched.")
      case None =>
        githubRepoInfoFut map {
          case Success(s) =>
            val repoDBobj = grater[Repository].asDBObject(Repository(s.getId, repo, new DateTime(1970, 1, 2, 0, 0), List.empty))
            repositoriesCollection.insert(repoDBobj)
          case Failure(f) => throw new RepoNotFoundException(s"Repository $repo does not exist on GitHub.")
        }
    }
  }

  /**
   * Validates that the repo string given by the user is like "user/repo"
   *
   * @param repo repository string
   * @return a tuple of (user, repo)
   */
  private def validateAndPairRepo(repo: String) = {
    val splitRepo = repo.split("/")
    if (splitRepo.size != 2)
      throw new IllegalArgumentException("Repository must have the form user/repo. Example: mdotson/metrics-dashboard")
    else
      (splitRepo.head, splitRepo.last)
  }
}
