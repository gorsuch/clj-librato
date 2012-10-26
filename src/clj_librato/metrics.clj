(ns clj-librato.metrics
  (:require [clj-json.core   :as json]
            [clj-http.client :as client]
            [clojure.string  :as string]
            clj-http.util))

(def uri-base "https://metrics-api.librato.com/v1/")

(defn uri
  "The full URI of a particular resource, by path fragments."
  [& path-fragments]
  (apply str uri-base 
         (interpose "/" (map clj-http.util/url-encode path-fragments))))

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
    :query-params (unparse-kw params)
    :throw-exceptions false})
   
  ([user api-key params body]
   (assoc (request user api-key params)
          :body (json/generate-string (unparse-kw body)))))

(defn collate [user api-key gauges counters]
  "Posts a set of gauges and counters."
  (client/post (uri "metrics")
               (request user api-key {} {:gauges gauges :counters counters})))

(defn get-metric
  "Gets a metric by name."
  ([user api-key name]
   (get-metric user api-key name {}))
  ([user api-key name params]
   (let [body (-> (client/get (uri "metrics" name)
                              (request user api-key params))
                :body
                json/parse-string
                parse-kw)]
     (assoc body :measurements
            (into {} (map (fn [[source measurements]]
                            [source (map parse-kw measurements)])
                          (:measurements body)))))))
