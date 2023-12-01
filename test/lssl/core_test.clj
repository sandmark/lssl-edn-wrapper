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
      (is (match? (m/in-any-order [{:cmd (m/all-of #"^cgf" #"Terminals" #"SetFilter" #"true")}
                                   {:cmd (m/all-of #"Doors" #"false")}])
                  (sut/init-key :filters {:only   [:terminals]
                                          :except [[:doors]]}))))

    (testing "Filters to Ship"
      (is (match? (m/in-any-order
                   [{:cmd (m/all-of #"SetFilterAction" #"Thrown" #"ToShip" #"true")}
                    {:cmd (m/all-of #"SetFilterAction" #"Toolgrip" #"ToShip" #"false")}])
                  (sut/init-key :filters-to-ship {:only [:thrown] :except [[:toolgrip]]}))))

    (testing "Hotkeys"
      (is (match?
           (m/embeds
            [{:cmd
              "hotkey F2 cgf \"LSSL:Interface.SetBoolControl\" \"PauseScan\" true; player.setav speedmult 400"}
             {:cmd
              "hotkey F3 cgf \"LSSL:Interface.CancelScan\""}])
           (sut/init-key :hotkeys {"F2" [[:set :pause-scan true]
                                         "player.setav speedmult 400"]
                                   "F3" [[:cancel-scan]]})))
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
                   [{:cmd (m/all-of #"Bypass" #"false")}
                    {:cmd (m/all-of #"Pick" #"false")}
                    {:cmd (m/all-of #"Search" #"true")}])
                  (sut/init-key :actions {:containers {:pick false :search true}
                                          :books      {:bypass false}}))))

    (testing "Priority"
      (is (match? (m/embeds [(m/all-of #"second" #(str/ends-with? % "last\";"))])
                  (sut/convert test-config))))

    (testing "Delimiter"
      (is (match? (m/embeds [#";$"])
                  (sut/convert test-config)))))

  (testing "Logging"
    (testing "Filter"
      (testing "Missing"
        (is (match? #"Lacking filter\(s\) found!"
                    (with-out-str
                      (sut/init {:lssl-config {:filters {:only [:custom]}}}))))
        (is (match? ""
                    (with-out-str
                      (let [[e & rest] (into [] sut/available-filters)]
                        (sut/init {:lssl-config
                                   {:filters {:only rest :except [[e]]}}})))))))))

(deftest edge-case-test
  (testing "SilentInterface"
    (testing "should be first"
      (let [settings (str {:lssl-config {:controls {:scan-radius 50.0 :silent-interface true}}})]
        (is (match? (m/prefix [#"SilentInterface"])
                    (sut/convert (java.io.StringReader. settings))))
        (is (match? [identity #"ScanRadius"]
                    (sut/convert (java.io.StringReader. settings))))))))
