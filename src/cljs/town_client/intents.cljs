(ns town-client.intents)

(defn highlight-neighborhood [nid source]
  {:type :highlight-neighborhood :id nid :source source})

(defn neighborhood-intent [id]
  {:type :newlocation, :location id })
