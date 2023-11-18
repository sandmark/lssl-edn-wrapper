(ns lssl.core-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lssl.core :as sut]
   [matcher-combinators.clj-test]
   [matcher-combinators.matchers :as m]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def test-config (io/resource "lssl-config-test.edn"))

(deftest conversion-test
  (testing "Conversion"
    (testing "Controls"
      (is (match? (m/in-any-order [{:cmd (m/all-of #"^cgf" #"ScanRadius" #"50\.0")}
                                   {:cmd (m/all-of #"AllowStealing" #"false")}])
                  (sut/init-key :controls {:scan-radius    50.0
                                           :allow-stealing false}))))
    (testing "Filters"
      (is (match? (m/in-any-order [{:cmd (m/all-of #"^cgf" #"Terminals" #"SetFilter" #"true$")}
                                   {:cmd (m/all-of #"Doors" #"false$")}])
                  (sut/init-key :filters {:only   [:terminals]
                                          :except [[:doors]]}))))

    (testing "Hotkeys"
      (is (match?
           (m/embeds
            [{:cmd "hotkey F2 cgf \"LSSL:Interface.SetBoolControl\" \"PauseScan\" true; player.setav speedmult 400"}])
           (sut/init-key :hotkeys {"F2" [[:set :pause-scan true]
                                         "player.setav speedmult 400"]})))
      (is (match? (m/prefix [(m/any-of {:cmd #"bUseConsoleHotkeys:Menu"})])
                  (sut/init-key :hotkeys {"A" ["A"]})))
      (is (match? (m/embeds [#"bUseConsoleHotkeys:Menu"])
                  (sut/convert test-config))))

    (testing "Startup"
      (is (match?
           [{:cmd      "cgf \"LSSL:Interface.AddToFilter\" \"AsFilter\" a; cgf \"Debug.Notification\" \"a added.\""
             :priority pos-int?}]
           (sut/init-key :startup [[:add :as-filter 0xa]
                                   [:message "a added."]]))))

    (testing "Actions"
      (is (match? (m/in-any-order
                   [{:cmd (m/all-of #"Bypass" #"false$")}
                    {:cmd (m/all-of #"Pick" #"false$")}
                    {:cmd (m/all-of #"Search" #"true$")}])
                  (sut/init-key :actions {:containers {:pick false :search true}
                                          :books      {:bypass false}}))))

    (testing "Priority"
      (is (match? (m/embeds [(m/all-of #"second" #(str/ends-with? % "last\""))])
                  (sut/convert test-config))))))
