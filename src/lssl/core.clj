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
            (if (map? v)
              (map->record-action attr var v)
              {:attribute attr :var (keyword->camel var) :value v}))]
    (map f m)))

(defn map->record [config]
  (flatten
   (map (fn [[attr m]]
          (if (map? m)
            (map->record-inner attr m)
            {:attribute attr :value m}))
        config)))

(defn double-quote [s] (str "\"" s "\""))

(defn operaterize [op]
  (if (keyword? op)
    (double-quote (str "LSSL:Interface." (keyword->camel op)))
    (double-quote op)))

(defn parameterize [param]
  (cond (keyword? param) (double-quote (str (keyword->camel param)))
        (string? param)  (double-quote param)
        (integer? param) (format "%x" param)
        :else            param))

(defn coll->papyrus [[op & args :as coll]]
  (if (vector? coll)
    (str/join " " (cons "cgf" (cons (operaterize op) (map parameterize args))))
    (str/join " " (cons op (map parameterize args)))))

(defn colls->papyrus [colls]
  (prn colls)
  (prn (map coll->papyrus colls))
  (str/join "; " (map coll->papyrus colls)))

(defn record-dispatch-fn [{:keys [:attribute :value]}]
  (cond (float? value)   [attribute :float]
        (vector? value)  [attribute :vector]
        (boolean? value) [attribute :boolean]))

(defmulti build-papyrus record-dispatch-fn)

(defmethod build-papyrus [:control :float]
  [{:keys [:var :value]}]
  (coll->papyrus [:set-float-control var value]))

(defmethod build-papyrus [:control :boolean]
  [{:keys [:var :value]}]
  (coll->papyrus [:set-bool-control var value]))

(defmethod build-papyrus [:filter :boolean]
  [{:keys [:value :var]}]
  (coll->papyrus [:set-filter var value]))

(defmethod build-papyrus [:filter-all :boolean]
  [{:keys [:value]}]
  (coll->papyrus [:set-all-filters value]))

(defmethod build-papyrus [:action :boolean]
  [{:keys [:var :action :value]}]
  (coll->papyrus [:set-filter-action var action value]))

(defmethod build-papyrus [:filter-add :vector]
  [{:keys [:var :value]}]
  (colls->papyrus (for [val value] [:add-to-filter var val])))

(defmethod build-papyrus [:filter-remove :vector]
  [{:keys [:var :value]}]
  (colls->papyrus (for [val value] [:remove-from-filter var val])))

(defmethod build-papyrus [:filter-exclude :vector]
  [{:keys [:var :value]}]
  (colls->papyrus (for [val value] [:add-to-exclusion var val])))

(defmethod build-papyrus [:filter-include :vector]
  [{:keys [:var :value]}]
  (colls->papyrus (for [val value] [:remove-from-exclusion var val])))

(defmethod build-papyrus [:hotkey :vector]
  [{:keys [:var :value]}]
  (if (or (vector? (first value))
          (list? (first value)))
    (format "hotkey %s %s" var (colls->papyrus value))
    (format "hotkey %s %s" var (coll->papyrus value))))

(defn convert [m]
  (->> m map->record (map build-papyrus)))
