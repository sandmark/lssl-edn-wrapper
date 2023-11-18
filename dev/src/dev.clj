(ns dev
  (:require
   [clojure.java.io :as io]
   [clojure.repl :refer :all]
   [clojure.spec.alpha :as s]
   [clojure.tools.namespace.repl :refer [refresh]]))

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")
