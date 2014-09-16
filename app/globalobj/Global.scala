package globalobj

import java.util.concurrent.TimeUnit

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.conversions.scala.{RegisterConversionHelpers, RegisterJodaTimeConversionHelpers}
import com.novus.salat._
import org.eclipse.egit.github.core.IRepositoryIdProvider
import org.joda.time.DateTime
import play.api.{Application, GlobalSettings, Play}
import globalobj.RemoteConnections._
import play.api.libs.concurrent.Execution.Implicits._

import model.{Commit, Repository}

import scala.concurrent._
import scala.concurrent.duration.Duration

object Global extends GlobalSettings {

  // needed to pick up Play's classloader for salat/casbah
  implicit lazy val ctx = new Context {
    val name = "Custom_Classloader"
  }

  /**
   * This is called when the application starts. Here we start the loop for adding commit history.
   *
   * @param app
   */
  override def onStart(app: Application) {
    // converters for salat
    RegisterConversionHelpers()
    RegisterJodaTimeConversionHelpers()
    // use play's class loader
    ctx.registerClassLoader(Play.classloader(Play.current))

    future {
      persistCommitHistorySince()
    }
  }

  /**
   * poll the github api for new commits for all repositories every hour
   */
  def persistCommitHistorySince() {

    // used for passing to SinceUtils
    case class MyRepo(fullRepoName: String) extends IRepositoryIdProvider {
      override def generateId() = fullRepoName
    }

    import lib.GitHubTimeUtils.CommitServiceImprovements
    import collection.JavaConversions._

    while (true) {

      val currentTime = DateTime.now()

      val allRepos = repositoriesCollection.find().toList

      allRepos map {
        // convert DBObject to Repository then update the repository in the DB
        dbObject =>
          val repo = grater[Repository].asObject(dbObject)

          // make object to make interface that getCommits expects
          val githubRepo = MyRepo(repo.full_name)

          // get all commits between repo's last_update time and current time
          val fullCommits = commitService.getCommits(githubRepo, null, null, repo.last_update, currentTime).toList

          // pull information out that we care about
          val commits = fullCommits map {
            commit => Commit(commit.getSha)
          }

          // update repository in database
          val repoWithTime = Repository(repo._id, repo.full_name, currentTime, repo.commits ++ commits)
          repositoriesCollection.update(dbObject, grater[Repository].asDBObject(repoWithTime))
      }

      blocking(Thread.sleep(Duration(10, TimeUnit.SECONDS).toMillis))
    }
  }
}