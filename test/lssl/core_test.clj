(ns lssl.core-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lssl.core :as sut]
   [matcher-combinators.clj-test]
   [matcher-combinators.matchers :as m]))

(deftest utils-test
  (testing "Keyword to CamelCase"
    (is (match? "HandScannerOnly"
                (sut/keyword->camel :hand-scanner-only))))

  (testing "Sort Records"
    (is (match? [{:order 0} {}]
                (sut/sort-records [{} {:order 0}])))))

(deftest structure-test
  (testing "Control map conversion"
    (is (match? [{:attribute :control :var "ScanRadius" :value 4.0}]
                (sut/map->record {:control {:scan-radius 4.0}})))
    (is (match? (m/in-any-order [{:attribute :action :var "Containers" :action "Pick" :value true}
                                 {:var "Containers" :action "Search" :value false}])
                (sut/map->record {:action {:containers {"Pick" true, "Search" false}}})))
    (is (match? [{:attribute :filter-add :var "Containers" :value [10 11]}]
                (sut/map->record {:filter-add {:containers [0xa 0xb]}})))
    (is (match? [{:attribute :filter-all :value true}]
                (sut/map->record {:filter-all true})))
    (is (match? [{:attribute :hotkey
                  :var       "F2"
                  :value     (m/in-any-order
                              [[:toggle-bool-control :pause-scan] [:cancel-scan] [:pour-anti-freeze]])}]
                (sut/map->record {:hotkey {"F2" [[:toggle-bool-control :pause-scan]
                                                 [:cancel-scan]
                                                 [:pour-anti-freeze]]}})))
    (testing "Ordering"
      (is (match? [{:order 0}]
                  (sut/map->record {:filter-all true}))))))

(deftest compile-papyrus-test
  (testing "Single cgf call"
    (is (match? "cgf \"LSSL:Interface.SetFloatControl\" \"ScanRadius\" 5.0"
                (sut/coll->papyrus [:set-float-control :scan-radius 5.0])))
    (is (match? "cgf \"LSSL:Debugger.PourAntiFreeze\""
                (sut/coll->papyrus ["LSSL:Debugger.PourAntiFreeze"]))))

  (testing "Multi cgf call"
    (is (match? "cgf \"LSSL:Interface.AddToFilter\" \"General\" f; cgf \"LSSL:Interface.AddToFilter\" \"General\" a"
                (sut/colls->papyrus [[:add-to-filter :general 0xf]
                                     [:add-to-filter :general 0xa]]))))

  (testing "Single raw call"
    (is (match? "player.setav speedmult 100"
                (sut/coll->papyrus '("player.setav speedmult 100")))))

  (testing "Multi raw call"
    (is (match? "cgf \"Debug.Notification\" \"Something.\"; player.setav speedmult 400"
                (sut/colls->papyrus [["Debug.Notification" "Something."]
                                     '("player.setav speedmult 400")])))))

(deftest lssl-compiler-test
  (testing "Control"
    (is (match? "cgf \"LSSL:Interface.SetFloatControl\" \"ScanRadius\" 5.0"
                (sut/build-papyrus {:attribute :control :var "ScanRadius" :value 5.0}))))

  (testing "Control Boolean"
    (is (match? "cgf \"LSSL:Interface.SetBoolControl\" \"PauseScan\" true"
                (sut/build-papyrus {:attribute :control :var "PauseScan" :value true}))))

  (testing "Filter"
    (is (match? "cgf \"LSSL:Interface.SetFilter\" \"Containers\" true"
                (sut/build-papyrus {:attribute :filter :var "Containers" :value true})))
    (is (match? "cgf \"LSSL:Interface.SetFilter\" \"EMWeap\" true"
                (sut/build-papyrus (first (sut/map->record {:filter {"EMWeap" true}})))))

    (testing "SetAllFilters"
      (is (match? "cgf \"LSSL:Interface.SetAllFilters\" true"
                  (sut/build-papyrus {:attribute :filter-all :value true}))))

    (testing "AddToFilter"
      (is (match? "cgf \"LSSL:Interface.AddToFilter\" \"Containers\" a"
                  (sut/build-papyrus {:attribute :filter-add :var "Containers" :value [10]})))
      (is (match? "cgf \"LSSL:Interface.AddToFilter\" \"Containers\" a; cgf \"LSSL:Interface.AddToFilter\" \"Containers\" b"
                  (sut/build-papyrus {:attribute :filter-add :var "Containers" :value [10 11]}))))

    (testing "RemoveFromFilter"
      (is (match? "cgf \"LSSL:Interface.RemoveFromFilter\" \"Containers\" a"
                  (sut/build-papyrus {:attribute :filter-remove :var "Containers" :value [10]}))))

    (testing "AddToExclusion"
      (is (match? "cgf \"LSSL:Interface.AddToExclusion\" \"Containers\" a"
                  (sut/build-papyrus {:attribute :filter-exclude :var "Containers" :value [10]}))))

    (testing "RemoveFromExclusion"
      (is (match? "cgf \"LSSL:Interface.RemoveFromExclusion\" \"Containers\" a"
                  (sut/build-papyrus {:attribute :filter-include :var "Containers" :value [10]})))))

  (testing "Action"
    (is (match? "cgf \"LSSL:Interface.SetFilterAction\" \"Containers\" \"Pick\" false"
                (sut/build-papyrus {:attribute :action :var "Containers" :action "Pick" :value false}))))

  (testing "Hotkey"
    (is (match? "hotkey F2 cgf \"LSSL:Interface.ToggleBoolControl\" \"PauseScan\"; cgf \"LSSL:Interface.CancelScan\"; cgf \"LSSL:Debugger.PourAntiFreeze\"; player.setav speedmult 100"
                (sut/build-papyrus {:attribute :hotkey :var "F2" :value [[:toggle-bool-control :pause-scan]
                                                                         [:cancel-scan]
                                                                         ["LSSL:Debugger.PourAntiFreeze"]
                                                                         '("player.setav speedmult 100")]})))
    (is (match? "hotkey F3 tgm; tcl"
                (sut/build-papyrus {:attribute :hotkey :var "F3" :value ['("tgm") '("tcl")]})))))
