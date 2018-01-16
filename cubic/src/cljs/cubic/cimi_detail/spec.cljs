(ns
  ^{:copyright "Copyright 2017, Charles A. Loomis, Jr."
    :license   "http://www.apache.org/licenses/LICENSE-2.0"}
  cubic.cimi-detail.spec
  (:require
    [clojure.spec.alpha :as s]))

(s/def ::loading? boolean?)

(s/def ::resource-id (s/nilable string?))

(s/def ::resource any?)

(s/def ::db (s/keys :req [::loading? ::resource-id ::resource]))

(def defaults {::loading?    true
               ::resource-id nil
               ::resource    nil})
