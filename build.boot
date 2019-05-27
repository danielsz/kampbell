(set-env!
 :source-paths  #{"src"}
 :resource-paths #{"src"}
 :dependencies '[[org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.logging "0.4.1"]
                 [inflections "0.13.2"]
                 [io.replikativ/konserve "0.5.0"]
                 [org.danielsz/lang-utils "0.1.2"]
                 [org.danielsz/maarschalk "0.1.3" :scope "test"]
                 [org.danielsz/system "0.4.4-SNAPSHOT" :scope "test"]
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [cider/cider-nrepl "0.22.0-beta1" :scope "test"]
                 [refactor-nrepl "2.4.0" :scope "test"]])

(require '[adzerk.boot-test :refer :all]
         '[cider.tasks :refer [add-middleware nrepl-server]])


(task-options!
 add-middleware {:middleware '[cider.nrepl/cider-middleware
                               refactor-nrepl.middleware/wrap-refactor]}
 push {:repo-map {:url "https://clojars.org/repo/"}}
 pom {:project 'org.danielsz/kampbell
      :version "0.1.7"
      :scm {:name "git"
            :url "https://github.com/danielsz/kampbell"}})

(deftask dev
  []
  (comp
   (watch :verbose true)
   (add-middleware)
   (nrepl-server)))

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
