# metrics-box


### Assumptions

* Postgres is running on localhost:5432 and has a 'metrics_box' database:

`create database metrics_box;`

* `metrics` & `metrics_history` tables have been created:

`metrics-box.postgresql/create-tables`

### Usage
`lein ring server`

API docs @ [localhost:3000/docs](http://localhost:3000/docs)

### Explanation

Step 1:

Writing out the endpoints based off input & output

* GET /metrics
  * input - nothing
  * output - array of all the metrics
* POST /metrics
  * input - metric obj (name, val, timestamp)
  * output -  created metric
* GET /value (value of metric at given time)
  * input - name, timestamp
  * output - value
  * 404 if metric does not exist
* GET /sum (aggregate value given a time range)
 *  input - name, start, end
 * output - value
 * 404 if metric does not exist

Step 2:

Datastore & Schema

As far as a datastores go I have the most experience with Postgres*. I created a `metrics` table for storing the metric names, and a separate `metrics_history` table for storing any pings to the server with a referenced metric's name.

`metrics`:
 ```
:id "serial PRIMARY KEY"
:name "varchar(32) UNIQUE NOT NULL"
 ```

`metrics_history`:
```
:metric_id "int REFERENCES metrics not null"
:value "double precision not null" (since a call exists to return an aggregate value this means it has to be a numerical value and a Double was the most inclusive choice)
:timestamp "timestamp not null"
```


Step 3:

Start coding!

I've used [Compojure-api](https://github.com/metosin/compojure-api) before for building production APIs and it's included schema support, destructuring of request params, and included Swagger UI for easy testing made it a simple choice. I used [Yesql](https://github.com/krisajenkins/yesql) as a SQL DSL since I knew I'd have some slight complicated joins to perform and usually prefer to keep SQL out of my code. When I realized I would need a `find_or_create` fn to check for an existing metric_id each time a new metric was posted, I checked the [Rails documentation](http://apidock.com/rails/v4.0.2/ActiveRecord/Relation/find_or_create_by) to see how they implemented this functionality. Lastly, as minor optimizations I used [HikariCP](https://github.com/tomekw/hikari-cp) as a JDBC connection pool so I wouldn't have to initialize new connections each time a query was made, [wrap-gzip](https://github.com/bertrandk/ring-gzip) to compress the response, and limit & offset support (with default values) for the /metrics endpoint.

\* re: Databases

 I had initially decided to use Datomic for this project as I had already used Postgres for a similar project, it was next on my list of databases to try, and it's default of storing all data changes would make it useful for the historical time range queries. I got through the [Datomic tutorial](http://docs.datomic.com/tutorial.html) and as far as creating a schema and writing out queries for 2/3 of the use-cases:

```
(def schema
  [{:db/id #db/id[:db.part/db]
    :db/ident :metric/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique :db.unique/identity
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :metric/value
    :db/valueType :db.type/float
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id #db/id[:db.part/db]
    :db/ident :metric/timestamp
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db.install/_attribute :db.part/db}])

(def query-value-by-name-and-timestamp
 '[:find (pull ?metric [:metric/value])
   :in $ ?name ?timestamp
   :where [?metric :metric/name ?name]                
          [?metric :metric/timestamp ?timestamp]])

(defn get-all-metrics
  []
 (d/q '[:find (pull ?metric [*])
        :where [?metric :metric/name]] (d/db conn)))
```

when I realized that querying the API for data based on a time range was a *primary* function, not something only done for auditing purposes. I realized I should be using a datastore tailored towards querying of historical data (vs my limited knowledge of Datomic accessing `datomic.api/history`) and switched to Postgres and redesigned the schema.
