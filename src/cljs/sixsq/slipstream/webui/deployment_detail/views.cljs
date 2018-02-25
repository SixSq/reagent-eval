(ns sixsq.slipstream.webui.deployment-detail.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [subscribe dispatch]]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [reagent.core :as r]

    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]

    [sixsq.slipstream.webui.main.events :as main-events]
    [sixsq.slipstream.webui.history.events :as history-events]
    [sixsq.slipstream.webui.dashboard.events :as dashboard-events]
    [sixsq.slipstream.webui.deployment-detail.events :as deployment-detail-events]
    [sixsq.slipstream.webui.deployment-detail.subs :as deployment-detail-subs]
    [sixsq.slipstream.webui.deployment-detail.utils :as deployment-detail-utils]
    [taoensso.timbre :as log]))


(defn ^:export set-runUUID [uuid]
  (dispatch [::deployment-detail-events/set-runUUID uuid]))


(defn section
  [title contents]
  [ui/Card {:fluid true}
   [ui/CardContent
    [ui/CardHeader (str title)]
    [ui/CardDescription
     contents]]])


(def summary-keys #{:creation
                    :category
                    :resourceUri
                    :lastStateChangeTime
                    :startTime
                    :endTime
                    :state
                    :type
                    :user
                    :cloudServiceNames})


(defn module-name
  [resource]
  (-> resource :run :module :name))


(defn format-parameter-key
  [k]
  (let [key-as-string (name k)]
    (if-let [abbrev-name (second (re-matches #"^.*:(.*)$" key-as-string))]
      abbrev-name
      key-as-string)))


(defn format-parameter-value
  [k v]
  (let [value (str v)]
    (if (re-matches #"^.*:url.*$" (name k))
      [:a {:href value} value]
      value)))


(defn tuple-to-row
  [[key value]]
  [ui/TableRow
   [ui/TableCell {:collapsing true} (format-parameter-key key)]
   [ui/TableCell {:style {:max-width     "80ex"             ;; FIXME: need to get this from parent container
                          :text-overflow "ellipsis"
                          :overflow      "hidden"}} (format-parameter-value key value)]])


(defn summary-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        cached-resource-id (subscribe [::deployment-detail-subs/cached-resource-id])
        resource (subscribe [::deployment-detail-subs/resource])]
    (fn []
      (let [module (module-name @resource)
            summary-info (-> (select-keys (:run @resource) summary-keys)
                             (assoc :uuid @cached-resource-id)
                             (assoc :module module))
            contents [ui/Table {:compact     true
                                :definition  true
                                :single-line true
                                :padded      false
                                :style       {:max-width "100%"}}
                      (vec (concat [ui/TableBody]
                                   (->> summary-info
                                        (map tuple-to-row))))]]
        [section (@tr [:summary]) contents]))))


(def node-parameter-pattern #"([^:]+?)(\.\d+)?:(.+)")


(defn parameter-section
  [[k _]]
  (when-let [[_ node] (re-matches node-parameter-pattern k)]
    node))


(defn node-parameter-table
  [params]
  [ui/Table {:compact     true
             :definition  true
             :single-line true
             :padded      false
             :style       {:max-width "100%"}}
   (vec (concat [ui/TableBody]
                (map tuple-to-row params)))])


(defn grouped-parameters
  [resource]
  (let [parameters (-> resource :run :runtimeParameters :entry)
        parameters-kv (->> parameters
                           (mapv (juxt :string #(-> % :runtimeParameter :content)))
                           (group-by parameter-section))]
    parameters-kv))


(defn parameters-dropdown
  [selected-section]
  (let [resource (subscribe [::deployment-detail-subs/resource])]
    (fn [selected-section]
      (let [parameter-groups (sort (keys (grouped-parameters @resource)))
            selection (if (contains? parameter-groups (set @selected-section))
                        @selected-section
                        "ss")]
        [ui/Dropdown
         {:options       (map #(identity {:key %, :text %, :value %}) parameter-groups)
          :selection     true
          :default-value "ss"
          :onChange      #(let [id (-> (js->clj %2 :keywordize-keys true) :value)]
                            (reset! selected-section id))}]))))


(defn parameters-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        resource (subscribe [::deployment-detail-subs/resource])
        selected-section (r/atom nil)]
    (fn []
      (let [parameters-kv (grouped-parameters @resource)
            parameter-group (get parameters-kv (or @selected-section "ss"))
            parameter-table (node-parameter-table parameter-group)
            contents (vec (concat [:div] [[parameters-dropdown selected-section]] [parameter-table]))]
        [section (@tr [:parameters]) contents]))))


(defn report-item
  [{:keys [id component created state] :as report}]
  ^{:key id} [:li
              (let [label (str/join " " [component created])]
                (if (= state "ready")
                  [:a {:onClick #(dispatch [::deployment-detail-events/download-report id])} label]
                  label))])


(def event-fields #{:id :content :timestamp :type})


(defn events-table-info
  [events]
  (when-let [start (-> events last :timestamp)]
    (let [dt-fn (partial deployment-detail-utils/assoc-delta-time start)]
      (->> events
           (map #(select-keys % event-fields))
           (map dt-fn)))))


(defn format-event-id
  [id]
  (let [tag (second (re-matches #"^.*/([^-]+).*$" id))]
    [:a {:on-click #(dispatch [::history-events/navigate (str "cimi/" id)])} tag]))


(defn event-map-to-row
  [{:keys [id content timestamp type delta-time] :as evt}]
  [ui/TableRow
   [ui/TableCell (format-event-id id)]
   [ui/TableCell timestamp]
   [ui/TableCell delta-time]
   [ui/TableCell type]
   [ui/TableCell (:state content)]])


(defn events-table
  [events]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [events]
      [ui/Table {:compact     true
                 :definition  false
                 :single-line true
                 :padded      false
                 :style       {:max-width "100%"}}
       [ui/TableHeader
        [ui/TableRow
         [ui/TableHeaderCell [:span (@tr [:event])]]
         [ui/TableHeaderCell [:span (@tr [:timestamp])]]
         [ui/TableHeaderCell [:span (@tr [:delta-min])]]
         [ui/TableHeaderCell [:span (@tr [:type])]]
         [ui/TableHeaderCell [:span (@tr [:state])]]]]
       (vec (concat [ui/TableBody]
                    (->> events
                         (map event-map-to-row))))])))


(defn events-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        events-collection (subscribe [::deployment-detail-subs/events])]
    (dispatch [::deployment-detail-events/fetch-events])
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :deployment-detail-events
                :frequency 30000
                :event     [::deployment-detail-events/fetch-events]}])
    (fn []
      (let [events (-> @events-collection :events events-table-info)]
        [section (@tr [:events])
         [events-table events]]))))


(defn reports-section
  []
  (let [tr (subscribe [::i18n-subs/tr])
        reports (subscribe [::deployment-detail-subs/reports])]
    (dispatch [::deployment-detail-events/fetch-reports])
    (dispatch [::main-events/action-interval
               {:action    :start
                :id        :deployment-detail-reports
                :frequency 30000
                :event     [::deployment-detail-events/fetch-reports]}])
    (fn []
      [section (@tr [:reports])
       [:div
        (vec
          (concat [:ul]
                  (vec (map report-item (:externalObjects @reports)))))
        [:p "Reports will be displayed as soon as available. No need to refresh."]]])))


(defn refresh-button
  []
  (let [loading? (subscribe [::deployment-detail-subs/loading?])
        runUUID (subscribe [::deployment-detail-subs/runUUID])]
    (fn []
      [ui/Button
       {:circular true
        :primary  true
        :icon     "refresh"
        :loading  @loading?
        :on-click #(dispatch [::deployment-detail-events/get-deployment @runUUID])}])))


(defn terminate-button
  []
  (let [tr (subscribe [::i18n-subs/tr])
        runUUID (subscribe [::deployment-detail-subs/runUUID])
        deployment (subscribe [::deployment-detail-resource])]
    (fn []
      [ui/Button
       {:negative       true
        :icon           true
        :label-position :left
        :on-click       #(log/error "TERMINATE:" @runUUID)
                        #_#(dispatch [::dashboard-events/delete-deployment-modal @deployment])}
       [ui/Icon {:name "close"}]
       (@tr [:terminate])])))


(defn service-link-button
  []
  (let [resource (subscribe [::deployment-detail-subs/resource])]
    (fn []
      (let [parameters-kv (grouped-parameters @resource)
            global-params (get parameters-kv "ss")
            state (second (first (filter #(= "ss:state" (first %)) global-params)))
            link (second (first (filter #(= "ss:url.service" (first %)) global-params)))]
        (log/error "Global params" global-params)
        (log/error state)
        (log/error link)
        (when (and link (= "Ready" state))
          [ui/Button
           {:primary        true
            :icon           true
            :label-position :left
            :on-click       #(set! js/window.location.href link)}
           [ui/Icon {:name "external"}]
           link])))))


(defn controls
  []
  (let [resource (subscribe [::deployment-detail-subs/resource])]
    (fn []
      (let [module (module-name @resource)
            short-name (second (re-matches #"^.*/(.*)$" (str (or module "TITLE"))))
            state (-> @resource :run :state)
            title (str short-name " (" state ")")]
        [section title [:div
                        [refresh-button]
                        [service-link-button]
                        [terminate-button]]]))))


(defn deployment-detail
  []
  (let [runUUID (subscribe [::deployment-detail-subs/runUUID])
        cached-resource-id (subscribe [::deployment-detail-subs/cached-resource-id])
        resource (subscribe [::deployment-detail-subs/resource])
        correct-resource? (and @runUUID (= @runUUID @cached-resource-id))]
    (fn []
      (when-not correct-resource?
        (dispatch [::deployment-detail-events/get-deployment @runUUID]))

      (vec
        (concat
          [:div]
          [[controls]]
          (when (and @runUUID @resource #_correct-resource?) ;; FIXME: Don't show information for a different resource.
            [[ui/Container {:fluid true}
              [summary-section]
              [parameters-section]
              [events-section]
              [reports-section]]]))))))
