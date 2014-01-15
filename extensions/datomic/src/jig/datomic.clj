(ns jig.datomic
  (:require
   jig
   [datomic.api :as d])
  (:import (jig Lifecycle)))

(deftype Connection [config]
  Lifecycle
  (init [_ system] system)

  (start [_ system]
    (let [uri (:uri config)]
      (d/create-database uri)
      (assoc-in system [(:jig/id config) :connection] (d/connect uri))))
  
  (stop [_ system] system))