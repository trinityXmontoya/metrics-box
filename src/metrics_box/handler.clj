(ns metrics-box.handler
  (:require [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [ring.util.http-response :refer :all]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.json :refer [wrap-json-response]]
            [metrics-box.postgresql :as pg]))

; SCHEMA
(s/defschema Metric
  {:name s/Str
   :value Double
   :timestamp org.joda.time.DateTime})

(s/defschema ChunkedMetrics
  {:total-metrics s/Int
   :total-results s/Int
   :offset s/Int
   :results [Metric]})

; HELPER
(defn handle-metric-existence-resp
  "abstraction for requests dependent on existence of metric by name"
  [name success-fn]
  (let [metric-id (:id (pg/get-metric-by-name name))]
    (if metric-id
      (ok (success-fn metric-id))
      (not-found (str "Metric '" name "' does not exist.")))))

; ROUTES
(defapi app-routes
  {:swagger
    {:ui "/docs"
     :spec "/swagger.json"}}
  (context "/metrics" []
    (GET "/" []
      :return ChunkedMetrics
      :query-params [{limit :- s/Int 50}
                     {offset :- s/Int 0}]
      (ok (pg/get-all-metrics limit offset)))
    (POST "/" []
      :body [metric Metric]
      :return Metric
      (ok (pg/create-metric-history metric)))
    (context "/:name" [name]
      (GET "/value" [name]
        :return (s/maybe Double)
        :query-params [timestamp :- org.joda.time.DateTime]
        (handle-metric-existence-resp name (partial pg/get-metric-value timestamp)))
      (GET "/sum" [name]
        :return (s/maybe Double)
        :query-params [start :- org.joda.time.DateTime
                       end :- org.joda.time.DateTime]
        (handle-metric-existence-resp name (partial pg/get-metric-sum start end))))))

(def app
  (-> app-routes
      wrap-json-response
      wrap-gzip))
