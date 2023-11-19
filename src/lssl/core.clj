(ns lssl.core
  (:require
   [aero.core :as aero]
   [lssl.processor :as p]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.set :as set]
   [taoensso.timbre :as log]))

(def load-hotkey-cmd "If getini \"bUseConsoleHotkeys:Menu\" == 0; setini \"bUseConsoleHotkeys:Menu\" 1; else cgf \"Debug.Notification\" \"LSSL edn wrapper: failed to load hotkey feature.\"; endif")

(def available-filters
  (->> [:custom     :terminals :doors   :containers :actors   :valuables :value-junk     :collectables :junk
        :contraband :resources :keys    :books      :aid      :chems     :food           :booze        :drinks :epic
        :apparel    :neuroamps :helmets :packs      :suits    :ammo      :assault-rifles :automatics   :ballistics
        :chemical   :cryo      "EMWeap" :explosives :fire     :heavy-gun :lasers         :melee        :mines
        :miniguns   :particle  :pistols :rifles     :shotguns :sniper    :thrown         :toolgrip     :unarmed
        :flora      :mining]
       (map p/keyword->camel)
       (into #{})))

(defmulti init-key
  (fn [k _] k))

(defmethod init-key :controls [_ controls]
  (->> controls
       (map (partial cons :set))
       (map p/transpile)))

(defn init-filters [k coll]
  (->> coll flatten (map (partial vector k)) (map p/transpile)))

(defn missing-filters [coll]
  (let [lacks (->> coll (map p/keyword->camel) (into #{}) (set/difference available-filters) (into []))]
    (when (seq lacks)
      lacks)))

(defmethod init-key :filters [_ {:keys [only except]}]
  (let [enabled  (init-filters :enable only)
        disabled (init-filters :disable except)]
    (when-let [lacks (missing-filters (flatten (concat only except)))]
      (log/warnf "Lacking filter(s) found! %s" lacks))
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
