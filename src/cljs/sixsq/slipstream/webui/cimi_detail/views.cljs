(ns sixsq.slipstream.webui.cimi-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe dispatch]]

    [sixsq.slipstream.webui.cimi.subs :as cimi-subs]
    [sixsq.slipstream.webui.cimi-detail.events :as cimi-detail-events]
    [sixsq.slipstream.webui.cimi-detail.subs :as cimi-detail-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.main.subs :as main-subs]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.resource-details :as details]
    [taoensso.timbre :as log]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        loading? (subscribe [::cimi-detail-subs/loading?])
        resource-id (subscribe [::cimi-detail-subs/resource-id])]
    (fn []
      [ui/MenuItem {:name "refresh"
                    :on-click #(dispatch [::cimi-detail-events/get @resource-id])}
       [ui/Icon {:name "refresh"
                 :loading @loading?}]
       (@tr [:refresh])])))


(defn path->resource-id
  [path]
  (str/join "/" (rest path)))


(defn cimi-detail
  []
  (let [cep (subscribe [::cimi-subs/cloud-entry-point])
        path (subscribe [::main-subs/nav-path])
        loading? (subscribe [::cimi-detail-subs/loading?])
        cached-resource-id (subscribe [::cimi-detail-subs/resource-id])
        resource (subscribe [::cimi-detail-subs/resource])
        description (subscribe [::cimi-detail-subs/description])]
    (fn []
      (let [resource-id (path->resource-id @path)
            correct-resource? (= resource-id @cached-resource-id)]

        ;; forces a refresh when the correct resource isn't cached
        (when-not correct-resource?
          (dispatch [::cimi-detail-events/get (path->resource-id @path)]))

        ;; render the (possibly empty) detail
        [details/resource-detail [refresh-button] resource-id
         (when (and (not @loading?) correct-resource?) @resource) (:baseURI @cep) @description]))))
