(ns sixsq.slipstream.webui.appstore.views
  (:require
    [clojure.string :as str]
    [re-frame.core :refer [dispatch subscribe]]
    [sixsq.slipstream.webui.appstore.events :as appstore-events]
    [sixsq.slipstream.webui.appstore.subs :as appstore-subs]
    [sixsq.slipstream.webui.cimi.subs :as cimi-subs]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.panel :as panel]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]
    [sixsq.slipstream.webui.utils.style :as style]
    [sixsq.slipstream.webui.utils.ui-callback :as ui-callback]
    [sixsq.slipstream.webui.utils.semantic-ui-extensions :as uix]
    [taoensso.timbre :as log]
    [sixsq.slipstream.webui.utils.general :as general-utils]
    [sixsq.slipstream.webui.utils.general :as utils]
    [reagent.core :as reagent]
    [sixsq.slipstream.webui.utils.time :as time]
    [sixsq.slipstream.webui.utils.form-fields :as ff]))



(defn refresh-button
  []
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn []
      [uix/MenuItemWithIcon
       {:name      (@tr [:refresh])
        :icon-name "refresh"
        :on-click  #(dispatch [::appstore-events/get-modules])}])))


(defn breadcrumb-path
  []
  (let [paths (subscribe [::appstore-subs/paths])
        parent-path-search (subscribe [::appstore-subs/parent-path-search])]
    (dispatch [::appstore-events/get-paths])
    (fn []
      (let [selected-paths (if (str/blank? @parent-path-search) [] (str/split @parent-path-search #"/"))]
        (log/error "@parent-path-search: " @parent-path-search selected-paths)
        (vec
          (concat
            [ui/Breadcrumb]
            [[ui/BreadcrumbSection [ui/Icon {:name     "home"
                                             :link     true
                                             :on-click #(dispatch [::appstore-events/set-parent-path-search ""])}]]
             [ui/BreadcrumbDivider {:icon "right angle"}]]
            (interpose
              [ui/BreadcrumbDivider {:icon "right angle"}]
              (concat
                (map (fn [path i]
                       [ui/BreadcrumbSection
                        [:a {:style    {:cursor "pointer"}
                             :on-click #(dispatch
                                          [::appstore-events/set-parent-path-search (->> selected-paths
                                                                                         (take i)
                                                                                         (str/join "/"))])}
                         path]])
                     selected-paths (drop 1 (range)))
                (when (not-empty @paths)
                  [^{:key @parent-path-search}
                  [ui/Dropdown {:scrolling   true
                                :placeholder "select project path"
                                :on-change   (ui-callback/value #(dispatch [::appstore-events/set-parent-path-search
                                                                            (if (str/blank? @parent-path-search)
                                                                              %
                                                                              (str @parent-path-search "/" %))]))
                                :options     @paths}]])
                ))))))))

(defn control-bar []
  (let [tr (subscribe [::i18n-subs/tr])
        cep (subscribe [::cimi-subs/cloud-entry-point])]
    [:div
     [ui/Menu {:attached "top", :borderless true}
      [refresh-button]
      [ui/MenuItem
       [breadcrumb-path]]
      [ui/MenuMenu {:position "right"}
       [ui/MenuItem
        [ui/Input {:placeholder (@tr [:search])
                   :icon        "search"
                   :on-change   (ui-callback/input-callback #(dispatch [::appstore-events/set-full-text-search %]))}]]
       ]]]))

(defn category-icon
  [category]
  (case category
    "PROJECT" "folder"
    "APPLICATION" "sitemap"
    "IMAGE" "file"
    "COMPONENT" "microchip"
    "question circle"))


(defn format-module
  [{:keys [id name description type parentPath logo] :as module}]
  (let [tr (subscribe [::i18n-subs/tr])]
    (fn [{:keys [id name description type parentPath logo] :as module}]
      ^{:key id}
      [ui/Card
       (when logo
         [ui/Image {:src   (:href logo),
                    :style {:width      "auto"
                            :height     "100px"
                            :object-fit "contain"}}])
       [ui/CardContent
        [ui/CardHeader {:style {:word-wrap "break-word"}}
         [ui/Icon {:name (category-icon type)}]
         (or name id)]
        [ui/CardMeta {:style {:word-wrap "break-word"}} parentPath]
        [ui/CardDescription {:style {:overflow "hidden" :max-height "100px"}} description]]
       [ui/Button {:fluid    true
                   :primary  true
                   :on-click #(dispatch [::appstore-events/open-deploy-modal module])}
        (@tr [:deploy])]])))


(defn modules-cards-group
  [modules-list]
  (when modules-list
    [ui/Segment style/basic
     (vec (concat [ui/CardGroup]
                  (map (fn [module] [format-module module]) modules-list)))]))


(defn deployment-template-list-item
  [{:keys [id name description created] :as tpl}]
  (let [selected-deployment-template (subscribe [::appstore-subs/selected-deployment-template])]
    ^{:key id}
    (let [{selected-id :id} @selected-deployment-template
          icon-name (if (= id selected-id) "check circle outline" "circle outline")]
      [ui/ListItem {:on-click #(dispatch [::appstore-events/set-selected-deployment-template tpl])}
       [ui/ListIcon {:name icon-name, :size "large", :vertical-align "middle"}]
       [ui/ListContent
        [ui/ListHeader (str (or name id) " (" (time/ago (time/parse-iso8601 created)) ")")]
        (or description "")]])))


(defn new-deployment-template-list-item
  []
  ^{:key "new-deployment-template-list-item"}
  [ui/ListItem {:on-click #(dispatch [::appstore-events/create-deployment-template])}
   [ui/ListIcon {:name "plus", :size "large", :vertical-align "middle"}]
   [ui/ListContent
    [ui/ListHeader "New Deployment Template"]
    "Create a new deployment template for this module."]])


(defn deployment-template-list
  []
  (let [deployment-templates (subscribe [::appstore-subs/deployment-templates])]
    (vec (concat [ui/ListSA {:divided   true
                             :relaxed   true
                             :selection true}
                  [new-deployment-template-list-item]]
                 (mapv deployment-template-list-item @deployment-templates)))))


(defn deployment-template-details
  []
  (let [template (subscribe [::appstore-subs/selected-deployment-template])
        text (reagent/atom (utils/edn->json (or @template {})))]
    (fn []
      (let [json-str (utils/edn->json (or @template {}))]
        (reset! text json-str)
        [:pre @text]

        ;; FIXME: Correct problem where the editor doesn't see external changes to the text.
        #_[editor/json-editor text]))))

(defn deployment-summary
  []
  (let [deploy-module (subscribe [::appstore-subs/deploy-module])]
    (let [{:keys [id name description path]} @deploy-module]
      [:div
       #_[format-module @deploy-module]
       [ui/ListSA
        [ui/ListItem id]
        [ui/ListItem name]
        [ui/ListItem description]
        [ui/ListItem path]
        ]])))


(defn deployment-resources
  []
  [ui/Form
   [ui/FormInput {:type "number", :label "CPU"}]
   [ui/FormInput {:type "number", :label "RAM"}]
   [ui/FormInput {:type "number", :label "DISK"}]
   ])


(defn template
  []
  (let [template (subscribe [::appstore-subs/selected-deployment-template])
        text (reagent/atom (utils/edn->json (or @template {})))]
    (fn []
      (let [json-str (utils/edn->json (or @template {}))]
        (reset! text json-str)
        [:pre @text]

        ;; FIXME: Correct problem where the editor doesn't see external changes to the text.
        #_[editor/json-editor text]))))


(defn as-form-input
  [{:keys [parameter description value] :as param}]
  (let [template (subscribe [::appstore-subs/selected-deployment-template])]
    ^{:key parameter}
    [ui/FormField
     [:label parameter ff/nbsp (ff/help-popup description)]
     [ui/Input
      {:type      "text"
       :name      parameter
       :read-only false
       :fluid     true
       :on-blur   (ui-callback/input-callback (fn [new-value]
                                                (let [updated-tpl (->> @template
                                                                       :module
                                                                       :content
                                                                       :inputParameters
                                                                       (remove #(= parameter (:parameter %)))
                                                                       (cons {:parameter   parameter
                                                                              :description description
                                                                              :value       new-value})
                                                                       vec
                                                                       (assoc-in @template [:module :content :inputParameters]))]
                                                  (dispatch [::appstore-events/set-selected-deployment-template updated-tpl]))))}]]))

(defn deployment-params
  []
  (let [template (subscribe [::appstore-subs/selected-deployment-template])]
    (let [params (-> @template :module :content :inputParameters)]
      (vec (concat [ui/Form]
                   (map as-form-input params))))))


(defn deploy-modal
  []
  (let [tr (subscribe [::i18n-subs/tr])
        visible? (subscribe [::appstore-subs/deploy-modal-visible?])
        deploy-module (subscribe [::appstore-subs/deploy-module])
        loading? (subscribe [::appstore-subs/loading-deployment-templates?])
        step-id (reagent/atom "params")]
    (fn []
      (let [hide-fn #(dispatch [::appstore-events/close-deploy-modal])
            submit-fn #(dispatch [::appstore-events/deploy])]
        [ui/Modal {:open       @visible?
                   :close-icon true
                   :on-close   hide-fn}

         [ui/ModalHeader [ui/Icon {:name "play"}] (@tr [:deploy]) " \u2014 " (:path @deploy-module)]

         [ui/ModalContent {:scrolling true}
          [ui/StepGroup {:attached "top"}
           [ui/Step {:icon        "check"
                     :title       "summary"
                     :description "one desc"
                     :on-click    #(reset! step-id "summary")
                     :active      (= "summary" @step-id)
                     }
            ]
           [ui/Step {:icon        "check"
                     :title       "template"
                     :description "one desc"
                     :on-click    #(reset! step-id "template-list")
                     :active      (= "template-list" @step-id)
                     }
            ]
           [ui/Step {:icon        "check"
                     :title       "resources"
                     :description "one desc"
                     :on-click    #(reset! step-id "resources")
                     :active      (= "resources" @step-id)
                     }
            ]
           [ui/Step {:icon        "check"
                     :title       "parameters"
                     :description "one desc"
                     :on-click    #(reset! step-id "params")
                     :active      (= "params" @step-id)
                     }
            ]]
          [ui/Segment {:attached true, :loading @loading?}
           (case @step-id
             "summary" [deployment-summary]
             "template-list" [deployment-template-list]
             "resources" [deployment-resources]
             "params" [deployment-params]
             nil)]
          #_[ui/Segment
             [deployment-template-details]]]

         [ui/ModalActions
          [uix/Button {:text     (@tr [:cancel]),
                       :on-click hide-fn
                       }]
          [uix/Button {:text     (@tr [:deploy]), :primary true,
                       :on-click #(do (hide-fn) (submit-fn))
                       }]]]))))

(defn module-resources
  []
  (let [modules (subscribe [::appstore-subs/modules])
        elements-per-page (subscribe [::appstore-subs/elements-per-page])
        page (subscribe [::appstore-subs/page])]
    (fn []
      (let [total-pages (general-utils/total-pages (get @modules :count 0) @elements-per-page)]
        [ui/Container {:fluid true}
         [control-bar]
         [deploy-modal]
         [modules-cards-group (:modules @modules)]
         (when (> total-pages 1)
           [uix/Pagination {:size         "tiny"
                            :totalPages   total-pages
                            :activePage   @page
                            :onPageChange (ui-callback/callback :activePage #(dispatch [::appstore-events/set-page %]))
                            }])]))))


(defmethod panel/render :appstore
  [path]
  (dispatch [::appstore-events/get-modules])
  [module-resources])