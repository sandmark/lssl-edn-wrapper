(ns lssl.core
  (:require [clojure.string :as str]))

(defn keyword->camel [k]
  (if (keyword? k)
    (->> (str/split (name k) #"\-")
         (map str/capitalize)
         str/join)
    k))

(defn- map->record-action [attribute var m]
  (map (fn [[action v]]
         {:attribute attribute
          :var       (keyword->camel var)
          :action    action
          :value     v})
       m))

(defn- map->record-inner [attr m]
  (letfn [(f [[var v]]
            (cond (map? v) (map->record-action attr var v)
                  :else    {:attribute attr :var (keyword->camel var) :value v}))]
    (map f m)))

(defn map->record [config]
  (flatten
   (map (fn [[attr m]] (map->record-inner attr m)) config)))

(defn dispatch-fn [{:keys [:attribute :value]}]
  (cond (float? value)   [attribute :float]
        (vector? value)  [attribute :vector]
        (boolean? value) [attribute :boolean]))

(defmulti build-papyrus dispatch-fn)

(defmethod build-papyrus [:control :float]
  [{:keys [:var :value]}]
  (format "cgf \"LSSL:Interface.SetFloatControl\" \"%s\" %s" var value))

(defmethod build-papyrus [:control :boolean]
  [{:keys [:var :value]}]
  (format "cgf \"LSSL:Interface.SetBoolControl\" \"%s\" %s" var value))

(defmethod build-papyrus [:filter :boolean]
  [{:keys [:value :var]}]
  (format "cgf \"LSSL:Interface.SetFilter\" \"%s\" %s" var value))

(defmethod build-papyrus [:action :boolean]
  [{:keys [:var :action :value]}]
  (format "cgf \"LSSL:Interface.SetFilterAction\" \"%s\" \"%s\" %s" var action value))

(defmethod build-papyrus [:filter-add :vector]
  [{:keys [:var :value]}]
  (str/join "; " (map #(format "cgf \"LSSL:Interface.AddToFilter\" \"%s\" %x" var %) value)))

(defmethod build-papyrus [:filter-remove :vector]
  [{:keys [:var :value]}]
  (str/join "; " (map #(format "cgf \"LSSL:Interface.RemoveFromFilter\" \"%s\" %x" var %) value)))

(defmethod build-papyrus [:filter-exclude :vector]
  [{:keys [:var :value]}]
  (str/join "; " (map #(format "cgf \"LSSL:Interface.AddToExclusion\" \"%s\" %x" var %) value)))

(defmethod build-papyrus [:filter-include :vector]
  [{:keys [:var :value]}]
  (str/join "; " (map #(format "cgf \"LSSL:Interface.RemoveFromExclusion\" \"%s\" %x" var %) value)))

(defn convert [m]
  (->> m map->record (map build-papyrus)))
