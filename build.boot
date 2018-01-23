(set-env!
 :source-paths  #{"src"}
 :resource-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/tools.logging "0.4.0"]
                 [inflections "0.13.0"]
                 [io.replikativ/konserve "0.4.11"]
                 [org.danielsz/lang-utils "0.1.0"]
                 [org.danielsz/maarschalk "0.1.0" :scope "test"]
                 [org.danielsz/system "0.4.2-SNAPSHOT" :scope "test"]
                 [adzerk/boot-test "1.2.0" :scope "test"]])

(require '[adzerk.boot-test :refer :all])

(task-options!
 push {:repo-map {:url "https://clojars.org/repo/"}}
 pom {:project 'org.danielsz/kampbell
      :version "0.1.4-SNAPSHOT"
      :scm {:name "git"
            :url "https://github.com/danielsz/kampbell"}})

(deftask dev
  []
  (comp (repl :server true) (watch)))

(deftask build
  []
  (comp (pom) (jar) (install)))

(deftask dev-checkout []
  (comp (watch) (build)))

(deftask push-release
  []
  (comp
   (build)
   (push)))

(deftask tests
  []
  (set-env! :source-paths #{"test"})
  (comp (test)))
