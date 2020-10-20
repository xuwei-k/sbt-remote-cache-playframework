scalaVersion := "2.13.3"

libraryDependencies +=  guice

enablePlugins(PlayScala)

pushRemoteCacheTo := Some(
  MavenCache("local-cache", (ThisBuild / baseDirectory).value / "remote-cache")
)

remoteCacheId := "fixed-id"

remoteCacheIdCandidates := Seq(remoteCacheId.value)

pushRemoteCacheConfiguration := pushRemoteCacheConfiguration.value.withOverwrite(true)

Compile / TwirlKeys.compileTemplates ~= { files =>
  val sourceInfo = "                  SOURCE: "
  val dateInfo = "                  DATE: "
  files.map { file =>
    val newSrc = IO.readLines(file).map { line =>
      if (line.startsWith(sourceInfo)) {
        sourceInfo
      } else if (line.startsWith(dateInfo)) {
        dateInfo
      } else {
        line
      }
    }.mkString("\n")
    IO.write(file, newSrc)
    file
  }
}

routesGenerator := {
  import play.routes.compiler.{RoutesCompiler, RoutesGenerator, Rule}
  val base = InjectedRoutesGenerator
  new RoutesGenerator {
    override def generate(
      task: RoutesCompiler.RoutesCompilerTask,
      namespace: Option[String],
      rules: List[Rule]
    ): Seq[(String, String)] = {
      val baseDir = (ThisBuild / baseDirectory).value.getCanonicalPath + "/"
      val sourceCommentPrefix = s"// @SOURCE:${baseDir}"
      base.generate(task = task, namespace = namespace, rules = rules).map {
        case (fileName, src) =>
          fileName -> src.linesIterator
            .filterNot(_ contains "@DATE")
            .map {
              case line if line.startsWith(sourceCommentPrefix) =>
                line.replace(baseDir, "")
              case line =>
                line
            }
            .mkString("\n")
      }
    }
    override def id = base.id
  }
}
