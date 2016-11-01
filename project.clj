(defproject metrics-box "0.1.0-SNAPSHOT"
  :description "See README"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;api
                 [metosin/compojure-api "1.1.9"]
                 [ring "1.5.0"]
                 [amalloy/ring-gzip-middleware "0.1.3"]
                 [ring/ring-json "0.4.0"]
                ;  formatter
                 [clj-time "0.12.0"]
                ;  postgres
                 [org.postgresql/postgresql "9.4-1206-jdbc42"]
                 [hikari-cp "1.7.5"]
                 [yesql "0.5.3"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler metrics-box.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}})
