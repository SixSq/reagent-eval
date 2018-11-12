(ns sixsq.slipstream.webui.deployment.spec
  (:require-macros [sixsq.slipstream.webui.utils.spec :refer [only-keys]])
  (:require
    [clojure.spec.alpha :as s]))


(s/def ::loading? boolean?)

(s/def ::active-only? boolean?)

(s/def ::deployments any?)

(s/def ::deployments-creds-map any?)

(s/def ::deployments-service-url-map any?)

(s/def ::page int?)

(s/def ::elements-per-page int?)

(s/def ::full-text-search (s/nilable string?))

(s/def ::db (s/keys :req [::loading?
                          ::deployments
                          ::page
                          ::elements-per-page
                          ::full-text-search
                          ::active-only?
                          ::deployments-creds-map
                          ::deployments-service-url-map]))

(def defaults {::loading?                    false
               ::page                        1
               ::elements-per-page           10
               ::full-text-search            nil
               ::active-only?                true
               ::deployments                 nil
               ::deployments-creds-map       {}
               ::deployments-service-url-map {}})
