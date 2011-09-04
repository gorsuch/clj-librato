(ns clj-librato.metrics
	(:require [clj-json.core :as json]
		  [clj-http.client :as client]))

(defn collate [user api-key gauges counters]
	(client/post "https://metrics-api.librato.com/v1/metrics.json"
		{:basic-auth [user api-key]
		 :body (json/generate-string {:gauges gauges :counters counters})
		 :content-type :json
		 :accept :json
		 :throw-exceptions false}))