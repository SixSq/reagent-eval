(ns sixsq.slipstream.webui.main.effects
  (:require
    [re-frame.core :refer [reg-fx dispatch]]
    [taoensso.timbre :as log]))

(defonce interval-handler                                   ;; we use of defonce to avoid creation of multiple interval
         (let [live-intervals (atom {})
               paused-intevals (atom {})]                    ;; handler at each figwheel reload
           (fn handler [[{:keys [action id frequency event] :as opts}]]
             (condp = action
               :clean (doseq [[id _] (map identity @live-intervals)]
                        (log/info "Cleaning " id)
                        (handler [{:action :end :id id}]))
               :pause (do
                        (log/info "Pausing intervals: " @live-intervals)
                        (reset! paused-intevals @live-intervals)
                        (handler [{:action :clean}]))
               :resume (do
                         (log/info "Resuming intervals: " @paused-intevals)
                         (reset! live-intervals @paused-intevals)
                         (doseq [[id opts] (map identity @live-intervals)]
                           (handler [opts])))
               :start (do
                        (log/info "Start dispatch timer: " opts)
                        (dispatch event)
                        (swap! live-intervals assoc id
                               (assoc opts :timer (js/setInterval #(dispatch event) frequency))))
               :end (do
                      (log/info "Will delete this timer: " (-> @live-intervals id :timer))
                      (js/clearInterval (-> @live-intervals id :timer))
                        (swap! live-intervals dissoc id))))))

;; when this code is reloaded `:clean` existing intervals
(interval-handler [{:action :clean}])

(reg-fx
  ::action-interval
  interval-handler)

