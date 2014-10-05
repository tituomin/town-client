(ns town-client.intents)

(defn highlight-neighborhood [nid source]
  {:type :highlight-neighborhood :id nid :source source})
