;; Copyright © 2013, JUXT LTD. All Rights Reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;;
;; By using this software in any fashion, you are agreeing to be bound by the
;; terms of this license.
;;
;; You must not remove this notice, or any other, from this software.

(require '(clojure [string :refer (join)]
                   [edn :as edn])
         '(clojure.java [shell :refer (sh)]
                        [io :as io]))

(def default-version "0.0.1-SNAPSHOT")

(defn head-ok []
  (-> (sh "git" "rev-parse" "--verify" "HEAD")
      :exit zero?))

(defn refresh-index []
  (sh "git" "update-index" "-q" "--ignore-submodules" "--refresh"))

(defn unstaged-changes []
  (-> (sh "git" "diff-files" "--quiet" "--ignore-submodules")
      :exit zero? not))

(defn uncommitted-changes []
  (-> (sh "git" "diff-index" "--cached" "--quiet" "--ignore-submodules" "HEAD" "--")
      :exit zero? not))

;; We don't want to keep having to 'bump' the version when we are
;; sitting on a more capable versioning system: git.
(defn get-version []
  (cond
   (not (let [gitdir (io/file ".git")]
          (and (.exists gitdir)
               (.isDirectory gitdir))))
   default-version

   (not (head-ok)) (throw (ex-info "HEAD not valid" {}))

   :otherwise
   (do
     (refresh-index)
     (let [{:keys [exit out err]} (sh "git" "describe" "--tags" "--long")]
       (if (= 128 exit) default-version
           (let [[[_ tag commits hash]] (re-seq #"(.*)-(.*)-(.*)" out)]
             (if (and
                  (zero? (edn/read-string commits))
                  (not (unstaged-changes))
                  (not (uncommitted-changes)))
               tag
               (let [[[_ stem lst]] (re-seq #"(.*\.)(.*)" tag)]
                 (join [stem (inc (read-string lst)) "-" "SNAPSHOT"])))))))))

(defproject jig (get-version)
  :description "A jig for developing systems using component composition. Based on Stuart Sierra's 'reloaded' workflow."
  :url "https://juxt.pro/jig"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 ;; Leiningen
                 [leiningen-core "2.1.0"]
                 ;; Tracing
                 [org.clojure/tools.trace "0.7.5"]
                 ;; Logging
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.7" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.2"]
                 [org.slf4j/jcl-over-slf4j "1.7.2"]
                 [org.slf4j/log4j-over-slf4j "1.7.2"]
                 ;; Graph algorithms for dependency graphs
                 [jkkramer/loom "0.2.0"]
                 ;; Pedestal!
                 [io.pedestal/pedestal.service "0.1.10"]
                 ;; (with jetty)
                 [io.pedestal/pedestal.jetty "0.1.10"]
                 ;; CSS for examples
                 [garden "0.1.0-beta6"]
                 ]

  ;; Only for core.async, remove when possible.
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}

  ;; Direct access to source project for now, but eventually use lein's aether to find projects.
  :source-paths ["src"
                 "examples/docsite/src"]

  :resource-paths ["resources"
                   "config"]

  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["repl"]}}
  )
