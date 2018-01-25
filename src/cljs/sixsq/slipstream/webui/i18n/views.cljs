(ns sixsq.slipstream.webui.i18n.views
  (:require
    [re-frame.core :refer [subscribe dispatch]]

    [sixsq.slipstream.webui.i18n.events :as i18n-events]
    [sixsq.slipstream.webui.i18n.subs :as i18n-subs]
    [sixsq.slipstream.webui.i18n.utils :as utils]
    [sixsq.slipstream.webui.utils.semantic-ui :as ui]))


(defn locale-dropdown-item
  [{:keys [value text]}]
  (let [on-click #(dispatch [::i18n-events/set-locale value])]
    [ui/DropdownItem {:on-click on-click} text]))


(defn locale-dropdown
  []
  (let [locale (subscribe [::i18n-subs/locale])]
    (fn []
      [ui/Dropdown {:class-name      "item"
                    :icon            "globe"
                    :pointing        "top right"
                    :close-on-change true}
       (vec (concat [ui/DropdownMenu]
                    (map locale-dropdown-item (utils/locale-choices))))])))