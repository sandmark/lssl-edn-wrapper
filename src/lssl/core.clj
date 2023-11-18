(ns lssl.core
  (:require
   [aero.core :as aero]
   [lssl.processor :as p]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def load-hotkey-cmd "If getini \"bUseConsoleHotkeys:Menu\" == 0; setini \"bUseConsoleHotkeys:Menu\" 1; else cgf \"Debug.Notification\" \"LSSL edn wrapper: failed to load hotkey feature.\"; endif")

(defmulti init-key
  (fn [k _] k))

(defmethod init-key :controls [_ controls]
  (->> controls
       (map (partial cons :set))
       (map p/transpile)))

(defn init-filters [k coll]
  (->> coll flatten (map (partial vector k)) (map p/transpile)))

(defmethod init-key :filters [_ {:keys [only except]}]
  (let [enabled  (init-filters :enable only)
        disabled (init-filters :disable except)]
    (concat enabled disabled)))

(defn ops->cmds [coll]
  (str/join "; " (map (comp :cmd p/transpile) coll)))

(defmethod init-key :hotkeys [_ m]
  (letfn [(->hotkey [[k ops]]
            {:cmd (format "hotkey %s %s" k (ops->cmds ops))})]
    (conj (map ->hotkey m) {:cmd load-hotkey-cmd})))

(defmethod init-key :startup [_ ops]
  [{:cmd (ops->cmds ops) :priority 10}])

(defmethod init-key :actions [_ m]
  (for [[asfilter settings] m
        [asaction bool]     settings]
    (p/transpile [:action asfilter asaction bool])))

(defn init [{:keys [lssl-config]}]
  (mapcat (fn [[k v]] (init-key k v)) lssl-config))

(defn sort-cmds [coll]
  (sort-by :priority coll))

(defn convert [f]
  (->> f aero/read-config init sort-cmds (map :cmd)))
