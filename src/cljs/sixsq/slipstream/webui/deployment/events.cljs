(ns sixsq.slipstream.webui.deployment.events
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]

    [sixsq.slipstream.webui.utils.general :as general-utils]

    [sixsq.slipstream.webui.cimi-api.effects :as cimi-api-fx]
    [sixsq.slipstream.webui.client.spec :as client-spec]
    [sixsq.slipstream.webui.deployment.spec :as deployment-spec]
    [sixsq.slipstream.webui.deployment.effects :as deployment-fx]))


(reg-event-db
  ::set-deployments
  (fn [db [_ deployments]]
    (assoc db ::deployment-spec/loading? false
              ::deployment-spec/deployments deployments)))


(reg-event-fx
  ::get-deployments
  (fn [{{:keys [::client-spec/client ::deployment-spec/query-params] :as db} :db} _]
    {:db                             (assoc db ::deployment-spec/loading? true)
     ::deployment-fx/get-deployments [client query-params]}))


(reg-event-db
  ::set-query-params
  (fn [db [_ params-map]]
    (update db ::deployment-spec/query-params merge params-map)))