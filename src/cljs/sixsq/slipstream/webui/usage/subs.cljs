(ns sixsq.slipstream.webui.usage.subs
  (:require
    [re-frame.core :refer [reg-sub]]
    [sixsq.slipstream.webui.usage.spec :as usage-spec]))

(reg-sub
  ::loading?
  ::usage-spec/loading?)

(reg-sub
  ::filter-visible?
  ::usage-spec/filter-visible?)

(reg-sub
  ::results
  ::usage-spec/results)

(reg-sub
  ::credentials-map
  ::usage-spec/credentials-map)

(reg-sub
  ::selected-credentials
  ::usage-spec/selected-credentials)

(reg-sub
  ::loading-credentials-map?
  ::usage-spec/loading-credentials-map?)

(reg-sub
  ::selected-users-roles
  ::usage-spec/selected-users-roles)

(reg-sub
  ::date-range
  ::usage-spec/date-range)
