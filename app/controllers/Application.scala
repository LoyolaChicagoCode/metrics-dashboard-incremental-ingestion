package controllers

import org.eclipse.egit.github.core.service.{CommitService, RepositoryService}
import org.json4s.jackson.JsonMethods
import play.api.mvc._
import dispatch._
import org.json4s._
import scala.concurrent.future

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Application extends Controller {

  val http = Http.configure(_ setFollowRedirects true)
  implicit val formats = DefaultFormats

  def index = Action {

//    val service = new RepositoryService()


//    val repository = future {
//    val stuff = service.getRepository("playframework", "playframework")
//    }

//    val commits = new CommitService().getCommits(stuff)
    /*
    val uri  = url("https://api.github.com/repos/mdotson/metrics-dashboard/commits")

    val responseFut = http(uri.GET)

    val response = Await.result(responseFut, Duration(5, "sec"))

    val json = JsonMethods.parse(response.getResponseBody)

    val commitHistory = json.camelizeKeys.extract[List[CommitInfo]]

    import org.json4s.jackson.Serialization
    val asJson = Serialization.write(commitHistory)

    Ok(views.html.index(response.getResponseBody, asJson))
    */
    Ok(views.html.index("1", "hi"))
  }

}