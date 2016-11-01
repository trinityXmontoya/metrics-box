(ns metrics-box.postgresql
  (:require [yesql.core :refer [defqueries]]
            [hikari-cp.core :as hcp]
            [clj-time.jdbc])
  (:gen-class))

; CONN CONFIG
(def db-spec {:adapter "postgresql"
              :server-name "localhost"
              :port-number 5432
              :database-name "metrics_box"})

(defn pg-pool
  [spec]
  (let [datasource (hcp/make-datasource db-spec)]
    {:datasource datasource}))

(def pg-conn (delay (pg-pool db-spec)))

(defqueries "sql/metrics.sql" {:connection @pg-conn})

; TABLE CONFIG
(defn create-tables
  []
  (create-metrics-table!)
  (create-metrics-history-table!))

; COMMANDS
(defn find-or-create-metric-by-name
  [name]
  (let [metric (first (query-by-name {:name name}))]
    (or metric (create-metric<! {:name name}))))

(defn create-metric-history
  [metric]
  (let [{:keys [name timestamp value]} metric
        metric-id (:id (find-or-create-metric-by-name name))
        params {:value value :timestamp  timestamp :metric_id metric-id}
        record (create-metric-history<! params)]
    metric))

(defn get-metric-by-name
  [name]
  (query-by-name {:name name}
                 {:result-set-fn first}))

(defn get-all-metrics
  [limit offset]
  (let [metrics-subset (query-all-metrics {:limit limit :offset offset})
        total-metrics (query-current-metric-count {}
                                                  {:result-set-fn first :row-fn :count})]
    {:total-metrics total-metrics
     :total-results (count metrics-subset)
     :offset offset
     :results metrics-subset}))

(defn get-metric-value
  [timestamp id]
  (query-by-timestamp {:metric_id id :timestamp timestamp}
                      {:result-set-fn first :row-fn :value}))

(defn get-metric-sum
  [start end id]
  (query-sum-by-range {:metric_id id :start start :end end}
                      {:result-set-fn first :row-fn :sum}))
