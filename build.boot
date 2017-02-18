(def +version+ "3.22-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/webui
  :version +version+
  :license {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :edition "community"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url
                                   lein-generate]])

(set-env!
  :source-paths #{"src/cljs"}
  :resource-paths #{"resources"}

  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-nexus-url)}]])

  :dependencies
  #(vec (concat %
                (merge-defaults
                  ['sixsq/default-deps (get-env :version)]
                  '[[org.clojure/clojure]
                    [org.clojure/clojurescript]

                    [binaryage/devtools]
                    [com.sixsq.slipstream/SlipStreamClientAPI-jar]
                    [com.taoensso/tempura]

                    [org.clojure/core.async]
                    
                    [reagent]
                    [re-frame]
                    [re-com]
                    
                    [adzerk/boot-cljs]
                    [adzerk/boot-cljs-repl]
                    [adzerk/boot-reload]
                    [adzerk/boot-test]
                    [boot-deps]
                    [com.cemerick/piggieback]
                    [crisptrutski/boot-cljs-test]
                    [doo]
                    [org.clojure/tools.nrepl]
                    [pandeiro/boot-http]
                    [tolitius/boot-check]
                    [weasel]]))))

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs]]
  '[pandeiro.boot-http :refer [serve]]
  '[tolitius.boot-check :refer [with-yagni
                                with-eastwood
                                with-kibit
                                with-bikeshed]]
  '[boot-deps :refer [ancient]])

(deftask build []
         (comp (speak)
               (cljs)))

(deftask run []
         (comp (serve)
               (watch)
               (cljs-repl)
               (reload)
               (build)))

(deftask production []
         (task-options! cljs {:optimizations    :advanced
                              :compiler-options {:pretty-print true
                                                 :pseudo-names true
                                                 }
                              })
         identity)

(deftask development []
         (task-options! cljs {:optimizations :none
                              :source-map true}
                        reload {:on-jsload 'sixsq.slipstream.webui/init})
         identity)

(deftask dev
         "Simple alias to run application in development mode"
         []
         (comp (development)
               (run)))


(deftask testing []
         (set-env! :source-paths #(conj % "test/cljs"))
         identity)

;;; This prevents a name collision WARNING between the test task and
;;; clojure.core/test, a function that nobody really uses or cares
;;; about.
(ns-unmap 'boot.user 'test)

(deftask test []
         (comp (testing)
               (test-cljs :js-env :phantom
                          :exit? true)))

(deftask deps [])

(deftask auto-test []
         (comp (testing)
               (watch)
               (test-cljs :js-env :phantom)))
