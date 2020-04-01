package healthhero.utils

import scala.io.Source

class ReadConfProperties {

    def getConfig(filePath: String) = {
        val source = Source.fromFile(filePath)
        try source.getLines()
              .filter(line => line.contains("="))
              .map { line =>
                        println(line)
                        val tokens = line.split("=")
                        (tokens(0) -> tokens(1))
                   }
          .toMap finally source.close()
    }

    /*def getBody(filePath: String) {
      val source = Source.fromFile(filePath)
      try {
        val body = source.getLines.mkString
          .replaceAll("\n", "")
          .replaceAll("\t", "")
        print(body)
        body.toString
      } finally source.close()
    }*/
}
