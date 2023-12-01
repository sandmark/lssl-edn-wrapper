(ns lssl.processor
  (:require [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn keyword->camel [k]
  (if (keyword? k)
    (->> (str/split (name k) #"\-")
         (map str/capitalize)
         str/join)
    k))

(defn papyrus-value [val]
  (cond (string? val)  (format "\"%s\"" val)
        (keyword? val) (recur (keyword->camel val))
        (map? val)     (format "%x" (:id val))
        :else          val))

(defn transpile-lssl [module method args]
  (let [module (keyword->camel module)
        method (keyword->camel method)
        args   (->> args (map papyrus-value) (str/join " "))]
    (str "cgf \"LSSL:" module "." method "\"" (when (seq args) (str " " args)))))

(defn transpile-interface [method & args]
  (transpile-lssl :interface method args))

(defn transpile-debugger [method & args]
  (transpile-lssl :debugger method args))

(defmulti transpile first)

(defmethod transpile :cancel-scan [[_]]
  {:cmd (transpile-interface :cancel-scan)})

(defmethod transpile :set [[_ control val :as call]]
  (if (= (keyword->camel control) "SilentInterface")
    {:priority -1 :cmd (transpile-interface :set-bool-control control val)}
    {:cmd
     (cond (int? val)     (transpile-interface :set-int-control control val)
           (float? val)   (transpile-interface :set-float-control control val)
           (boolean? val) (transpile-interface :set-bool-control control val)
           :else          (log/error "Invalid value:" call))}))

(defmethod transpile :toggle-control [[_ control]]
  {:cmd (transpile-interface :toggle-bool-control control)})

(defmethod transpile :try-scan-now [[_ opts]]
  (let [skip-hand-scanner       (get opts :skip-hand-scanner false)
        skip-location-exclusion (get opts :skip-location-exclusion false)]
    {:cmd (transpile-interface :try-scan-now skip-location-exclusion skip-hand-scanner)}))

(defmethod transpile :enable-all [[_ bool]]
  {:cmd (transpile-interface :set-all-filters bool)})

(defmethod transpile :enable [[_ asfilter]]
  {:cmd (transpile-interface :set-filter asfilter true)})

(defmethod transpile :disable [[_ asfilter]]
  {:cmd (transpile-interface :set-filter asfilter false)})

(defmethod transpile :toggle [[_ asfilter]]
  {:cmd (transpile-interface :toggle-filter asfilter)})

(defmethod transpile :action [[_ asfilter-or-asaction action-or-bool bool?]]
  {:cmd
   (if (boolean? action-or-bool)
     (transpile-interface :set-all-filters-by-action asfilter-or-asaction action-or-bool)
     (transpile-interface :set-filter-action asfilter-or-asaction action-or-bool bool?))})

(defmethod transpile :mod [[_ control val]]
  {:cmd (transpile-interface :mod-int-control control val)})

(defmethod transpile :add [[_ asfilter id]]
  {:cmd (transpile-interface :add-to-filter asfilter {:id id})})

(defmethod transpile :exclude [[_ asfilter id]]
  {:cmd (transpile-interface :add-to-exclusion asfilter {:id id})})

(defmethod transpile :remove [[_ asfilter id]]
  {:cmd (transpile-interface :remove-from-filter asfilter {:id id})})

(defmethod transpile :include [[_ asfilter id]]
  {:cmd (transpile-interface :remove-from-exclusion asfilter {:id id})})

(defmethod transpile :export [[_]]
  {:cmd (transpile-interface :generate-settings-export)})

(defmethod transpile :dump [[_ id]]
  {:cmd (transpile-debugger :dump-object-info {:id id})})

(defmethod transpile :dump-extended [[_]]
  {:cmd (transpile-debugger :dump-extended-filters-and-exclusions)})

(defmethod transpile :raw [[_ & cmds]]
  {:cmd (str/join "; " cmds)})

(defmethod transpile :message [[_ msg]]
  {:cmd (format "cgf \"Debug.Notification\" \"%s\"" msg)})

(defmethod transpile :reset [[_ x]]
  {:cmd (transpile-debugger :reset-looted-flag-in-area x)})

(defmethod transpile :query-state [[_ control]]
  {:cmd (transpile-interface :query-state control)})

(defmethod transpile :default [v]
  {:cmd v})
