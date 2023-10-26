(ns lssl.test_core
  (:require [clojure.test :as t]
            [lssl.core :as suit]))

(t/deftest utils-test
  (t/testing "Keyword to CamelCase"
    (t/is (= "HandScannerOnly" (suit/keyword->camel :hand-scanner-only))))

  (t/testing "Sort Records"
    (t/is (= [{:order 0} {}]
             (suit/sort-records [{} {:order 0}])))))

(t/deftest structure-test
  (t/testing "Control map conversion"
    (t/is (= [{:attribute :control :var "ScanRadius" :value 4.0}]
             (suit/map->record {:control {:scan-radius 4.0}})))
    (t/is (= [{:attribute :action :var "Containers" :action "Pick" :value true}
              {:attribute :action :var "Containers" :action "Search" :value false}]
             (suit/map->record {:action {:containers {"Pick" true, "Search" false}}})))
    (t/is (= [{:attribute :filter-add :var "Containers" :value [10 11]}]
             (suit/map->record {:filter-add {:containers [0xa 0xb]}})))
    (t/is (= [{:attribute :filter-all :value true :order 0}]
             (suit/map->record {:filter-all true})))
    (t/is (= [{:attribute :hotkey
               :var       "F2"
               :value     [[:toggle-bool-control :pause-scan] [:cancel-scan] [:pour-anti-freeze]]}]
             (suit/map->record {:hotkey {"F2" [[:toggle-bool-control :pause-scan]
                                               [:cancel-scan]
                                               [:pour-anti-freeze]]}})))
    (t/testing "Ordering"
      (t/is (= [{:attribute :filter-all :order 0 :value true}]
               (suit/map->record {:filter-all true}))))))

(t/deftest compile-papyrus-test
  (t/testing "Single cgf call"
    (t/is (= "cgf \"LSSL:Interface.SetFloatControl\" \"ScanRadius\" 5.0"
             (suit/vec->cgf [:set-float-control :scan-radius 5.0])))
    (t/is (= "cgf \"LSSL:Debugger.PourAntiFreeze\""
             (suit/vec->cgf ["LSSL:Debugger.PourAntiFreeze"]))))

  (t/testing "Multi cgf call"
    (t/is (= "cgf \"LSSL:Interface.AddToFilter\" \"General\" f; cgf \"LSSL:Interface.AddToFilter\" \"General\" a"
             (suit/vecs->cgf [[:add-to-filter :general 0xf]
                              [:add-to-filter :general 0xa]]))))

  (t/testing "Single raw call"
    (t/is (= "player.setav speedmult 100"
             (suit/coll->papyrus '("player.setav speedmult 100")))))

  (t/testing "Multi raw call"
    (t/is (= "cgf \"Debug.Notification\" \"Something.\"; player.setav speedmult 400"
             (suit/colls->papyrus [["Debug.Notification" "Something."]
                                   '("player.setav speedmult 400")])))))

(t/deftest lssl-compiler-test
  (t/testing "Control"
    (t/is (= "cgf \"LSSL:Interface.SetFloatControl\" \"ScanRadius\" 5.0"
             (suit/build-papyrus {:attribute :control :var "ScanRadius" :value 5.0}))))

  (t/testing "Control Boolean"
    (t/is (= "cgf \"LSSL:Interface.SetBoolControl\" \"PauseScan\" true"
             (suit/build-papyrus {:attribute :control :var "PauseScan" :value true}))))

  (t/testing "Filter"
    (t/is (= "cgf \"LSSL:Interface.SetFilter\" \"Containers\" true"
             (suit/build-papyrus {:attribute :filter :var "Containers" :value true})))
    (t/is (= "cgf \"LSSL:Interface.SetFilter\" \"EMWeap\" true"
             (suit/build-papyrus (first (suit/map->record {:filter {"EMWeap" true}})))))

    (t/testing "SetAllFilters"
      (t/is (= "cgf \"LSSL:Interface.SetAllFilters\" true"
               (suit/build-papyrus {:attribute :filter-all :value true}))))

    (t/testing "AddToFilter"
      (t/is (= "cgf \"LSSL:Interface.AddToFilter\" \"Containers\" a"
               (suit/build-papyrus {:attribute :filter-add :var "Containers" :value [10]})))
      (t/is (= "cgf \"LSSL:Interface.AddToFilter\" \"Containers\" a; cgf \"LSSL:Interface.AddToFilter\" \"Containers\" b"
               (suit/build-papyrus {:attribute :filter-add :var "Containers" :value [10 11]}))))

    (t/testing "RemoveFromFilter"
      (t/is (= "cgf \"LSSL:Interface.RemoveFromFilter\" \"Containers\" a"
               (suit/build-papyrus {:attribute :filter-remove :var "Containers" :value [10]}))))

    (t/testing "AddToExclusion"
      (t/is (= "cgf \"LSSL:Interface.AddToExclusion\" \"Containers\" a"
               (suit/build-papyrus {:attribute :filter-exclude :var "Containers" :value [10]}))))

    (t/testing "RemoveFromExclusion"
      (t/is (= "cgf \"LSSL:Interface.RemoveFromExclusion\" \"Containers\" a"
               (suit/build-papyrus {:attribute :filter-include :var "Containers" :value [10]})))))

  (t/testing "Action"
    (t/is (= "cgf \"LSSL:Interface.SetFilterAction\" \"Containers\" \"Pick\" false"
             (suit/build-papyrus {:attribute :action :var "Containers" :action "Pick" :value false}))))

  (t/testing "Hotkey"
    (t/is (= "hotkey F2 cgf \"LSSL:Interface.ToggleBoolControl\" \"PauseScan\"; cgf \"LSSL:Interface.CancelScan\"; cgf \"LSSL:Debugger.PourAntiFreeze\"; player.setav speedmult 100"
             (suit/build-papyrus {:attribute :hotkey :var "F2" :value [[:toggle-bool-control :pause-scan]
                                                                       [:cancel-scan]
                                                                       ["LSSL:Debugger.PourAntiFreeze"]
                                                                       '("player.setav speedmult 100")]})))
    (t/is (= "hotkey F3 tgm; tcl"
             (suit/build-papyrus {:attribute :hotkey :var "F3" :value ['("tgm") '("tcl")]})))))
