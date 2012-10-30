(ns clj-librato.metrics
  (:use [slingshot.slingshot :only [try+]])
  (:require [clj-json.core   :as json]
            [clj-http.client :as client]
            [clojure.string  :as string]
            clj-http.util))

(def uri-base "https://metrics-api.librato.com/v1/")

(defn uri
  "The full URI of a particular resource, by path fragments."
  [& path-fragments]
  (apply str uri-base 
         (interpose "/" (map (comp clj-http.util/url-encode str) 
                             path-fragments))))

(defn unparse-kw
  "Convert a clojure-style dashed keyword map into string underscores.
  Recursive."
  [m]
  (cond
    (map? m) (into {} (map (fn [[k v]] 
                             [(string/replace (name k) "-" "_") 
                              (unparse-kw v)]) 
                           m))
    (coll? m) (map unparse-kw m)
    true m))

(defn parse-kw
  "Parse a response map into dashed, keyword keys. Not recursive: some librato
  API functions return arbitrary string keys in maps."
  [m]
  (into {} (map (fn [[k v]] [(keyword (string/replace k "_" "-")) v]) m)))

(defn request
  "Constructs the HTTP client request map."
  ([user api-key params]
   {:basic-auth [user api-key]
    :content-type :json
    :accept :json
    :throw-entire-message? true
    :query-params (unparse-kw params)})
   
  ([user api-key params body]
   (assoc (request user api-key params)
          :body (json/generate-string (unparse-kw body)))))

(defn collate [user api-key gauges counters]
  "Posts a set of gauges and counters."
  (client/post (uri "metrics")
               (request user api-key {} {:gauges gauges :counters counters})))

(defn metric
  "Gets a metric by name.
  
  See http://dev.librato.com/v1/get/metrics"
  ([user api-key name]
   (metric user api-key name {}))

  ([user api-key name params]
   (assert name)
   (try+
     (let [body (-> (client/get (uri "metrics" name)
                                (request user api-key params))
                  :body json/parse-string parse-kw)]
       (assoc body :measurements
              (into {} (map (fn [[source measurements]]
                              [source (map parse-kw measurements)])
                            (:measurements body)))))
     (catch [:status 404] _
       (prn "caught 404")
       nil))))

(defn create-annotation
  "Creates a new annotation, and returns the created annotation as a map.
  
  http://dev.librato.com/v1/post/annotations/:name"
  [user api-key name annotation]
  (assert name)
  (-> (client/post (uri "annotations" name)
                   (request user api-key {} annotation))
    :body
    json/parse-string
    parse-kw))

(defn update-annotation
  "Updates an annotation.
  
  http://dev.librato.com/v1/put/annotations/:name/events/:id"
  [user api-key name id annotation]
  (assert name)
  (assert id)
  (client/put (uri "annotations" name id)
              (request user api-key {} annotation)))

(defn annotate
  "Creates or updates an annotation. If id is given, updates. If id is
  missing, creates a new annotation."
  ([user api-key name annotation]
   (create-annotation user api-key name annotation))
  ([user api-key name id annotation]
   (update-annotation user api-key name id annotation)))

(defn annotation
  "Find a particular annotation event.

  See http://dev.librato.com/v1/get/annotations/:name/events/:id"
  [user api-key name id]
  (assert name)
  (assert id)
  (try+
    (-> (client/get (uri "annotations" name id) (request user api-key {}))
      :body
      json/parse-string
      parse-kw)
    (catch [:status 404] _ nil)))
