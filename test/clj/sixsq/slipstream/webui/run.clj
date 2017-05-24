(ns sixsq.slipstream.webui.run
  (:require [ring.util.response :as r]))

(defn index-handler
  "For GET requests, always serves index.html from classpath. If index.html
   cannot be found, then a 404 is returned. Any method other than GET will
   return a 405 response."
  [{:keys [request-method] :as request}]
  (if (= request-method :get)
    (let [index-html "webui/index.html"]
      (if-let [resp (r/resource-response index-html)]
        (r/content-type resp "text/html")
        (r/not-found index-html)))
    (-> (r/response "method not allowed")
        (r/status 405))))
