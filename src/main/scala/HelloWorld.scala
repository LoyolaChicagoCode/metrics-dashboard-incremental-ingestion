import java.util.concurrent.TimeUnit

import com.mongodb.casbah.commons.conversions.scala.{RegisterJodaTimeConversionHelpers, RegisterConversionHelpers}
import com.novus.salat._
import org.eclipse.egit.github.core.{RepositoryCommit, IRepositoryIdProvider}
import org.joda.time.DateTime
import model.{Commit, Repository}

import scala.concurrent._
import scala.concurrent.duration.Duration
import RemoteConnections._

import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.format.ISODateTimeFormat
import com.novus.salat.{TypeHintFrequency, StringTypeHintStrategy, Context}
import com.novus.salat.json.{StringDateStrategy, JSONConfig}

import com.mongodb.casbah.Imports._

/**
 * Created by mdotson on 12/17/14.
 */
object HelloWorld {
  // needed to pick up Play's classloader for salat/casbah


  def main(args: Array[String]) {

    persistCommitHistorySince()
  }

  /**
   * poll the github api for new commits for all repositories every hour
   */
  def persistCommitHistorySince() {

//    implicit lazy val ctx = new Context {
//      val name = "Custom_Classloader"
//    }

    // converters for salat
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()

    import com.novus.salat.global._

    // used for passing to SinceUtils
    case class MyRepo(fullRepoName: String) extends IRepositoryIdProvider {
      override def generateId() = fullRepoName
    }

    import lib.GitHubTimeUtils.CommitServiceImprovements
    import collection.JavaConversions._

    while (true) {

      println("start")

      val currentTime = DateTime.now()

      val allRepos = repositoriesCollection.find().toList

      allRepos map {
        // convert DBObject to Repository then update the repository in the DB
        dbObject =>
          val repo = grater[Repository].asObject(dbObject)

          // make object to make interface that getCommits expects
          val githubRepo = MyRepo(repo._id)

          println("getting commits for " + repo._id)
          // get all commits between repo's last_update time and current time
          val fullCommits: List[RepositoryCommit] = try {
            commitService.getCommits(githubRepo, null, null, DateTime.parse(repo.last_updated), currentTime).toList
          } catch {
            case e: Exception => println("ugh: " + e.getMessage); List.empty
          }
          println("got commits")
          println("limit: " + githubClient.getRemainingRequests)

          // pull information out that we care about
          val commits = fullCommits map {
            commit => Commit(commit.getSha)
          }

          if (commits.size != 0) {
            println("new-commits: " + commits.size + " for " + repo._id)
            sys.exit(0)
          }
          // update repository in database
          val repoWithTime = Repository(repo._id, repo.commit_count, currentTime.toString)
          repositoriesCollection.update(dbObject, grater[Repository].asDBObject(repoWithTime))
      }

      blocking(Thread.sleep(Duration(60, TimeUnit.SECONDS).toMillis))
    }
  }
}
