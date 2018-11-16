(ns sixsq.slipstream.webui.data.views
  (:require
    [re-frame.core :refer [dispatch subscribe]]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.application.utils :as application-utils]
    [sixsq.slipstream.webui.appstore.events :as appstore-events]
    [sixsq.slipstream.webui.appstore.views :as appstore-views]
    [sixsq.slipstream.webui.data.events :as events]
    [sixsq.slipstream.webui.data.subs :as subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.time :as time]))


(defn refresh []
  (dispatch [::events/get-content-types])
  (dispatch [::events/get-credentials]))


(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :on-click  refresh}])))


(defn search-header []
  (let [tr (subscribe [::i18n-subs/tr])
        time-period (subscribe [::subs/time-period])
        locale (subscribe [::i18n-subs/locale])]
    (fn []
      (let [[time-start time-end] @time-period]
        [ui/Form {:widths "equal"}
         [ui/FormGroup
          [ui/FormField
           ;; FIXME: Find a better way to set the field width.
           [ui/DatePicker {:custom-input     (reagent/as-element [ui/Input {:label (@tr [:from])
                                                                            :style {:min-width "25em"}}])
                           :selected         time-start
                           :start-date       time-start
                           :end-date         time-end
                           :max-date         time-end
                           :selects-start    true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch [::events/set-time-period [% time-end]])
                           }]]
          ;; FIXME: Find a better way to set the field width.
          [ui/FormField
           [ui/DatePicker {:custom-input     (reagent/as-element [ui/Input {:label (@tr [:to])
                                                                            :style {:min-width "25em"}}])
                           :selected         time-end
                           :start-date       time-start
                           :end-date         time-end
                           :min-date         time-start
                           :max-date         (time/now)
                           :selects-end      true
                           :show-time-select true
                           :time-format      "HH:mm"
                           :time-intervals   15
                           :locale           @locale
                           :fixed-height     true
                           :date-format      "LLL"
                           :on-change        #(dispatch [::events/set-time-period [time-start %]])
                           }]]]
         ]))))

(defn control-bar []
  [:div
   [ui/Menu {:attached "top", :borderless true}
    [refresh-button]]
   [ui/Segment {:attached "bottom"}
    [search-header]]])


(defn application-list-item
  [{:keys [id name description type created] :as application}]
  ^{:key id}
  [ui/ListItem {:on-click #(do
                             (dispatch [::events/close-application-select-modal])
                             (dispatch [::appstore-events/create-deployment id "data"]))}
   [ui/ListIcon {:name (application-utils/category-icon type), :size "large", :vertical-align "middle"}]
   [ui/ListContent
    [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
    (or description "")]])


(defn application-list
  []
  (let [applications (subscribe [::subs/applications])]
    (vec (concat [ui/ListSA {:divided   true
                             :relaxed   true
                             :selection true}]
                 (mapv application-list-item @applications)))))

(defn application-select-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::subs/application-select-visible?])]
    (fn []
      (let [hide-fn #(dispatch [::events/close-application-select-modal])]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "play"}] (@tr [:select-application])]

         [ui/ModalContent {:scrolling true}
          [ui/ModalDescription
           [application-list]]]]))))


(defn format-content-type
  [{:keys [key doc_count] :as content-type}]
  (let [tr (subscribe [::i18n-subs/tr])]
    ^{:key key}
    [ui/Card
     [ui/CardContent
      [ui/CardHeader {:style {:word-wrap "break-word"}} key]
      [ui/CardMeta {:style {:word-wrap "break-word"}} (@tr [:count]) ": " doc_count]]
     [ui/Button {:fluid    true
                 :primary  true
                 :on-click #(dispatch [::events/open-application-select-modal key])}
      (@tr [:process])]]))


(defn content-types-cards-group
  []
  (let [content-types (subscribe [::subs/content-types])]
    [ui/Segment style/basic
     (vec (concat [ui/CardGroup]
                  (map (fn [content-type]
                         [format-content-type content-type])
                       @content-types)))]))


(defn service-offer-resources
  []
  [ui/Container {:fluid true}
   [control-bar]
   [application-select-modal]
   [appstore-views/deploy-modal true]
   [content-types-cards-group]])


(defmethod panel/render :data
  [path]
  (refresh)
  [service-offer-resources])
