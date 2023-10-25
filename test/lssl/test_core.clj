(ns lssl.test_core
  (:require [clojure.test :as t]
            [lssl.core :as suit]))

(t/deftest utils-test
  (t/testing "Keyword to CamelCase"
    (t/is (= "HandScannerOnly" (suit/keyword->camel :hand-scanner-only)))))

(t/deftest structure-test
  (t/testing "Control map conversion"
    (t/is (= [{:attribute :control :var "ScanRadius" :value 4.0}]
             (suit/map->record {:control {:scan-radius 4.0}})))
    (t/is (= [{:attribute :action :var "Containers" :action "Pick" :value true}
              {:attribute :action :var "Containers" :action "Search" :value false}]
             (suit/map->record {:action {:containers {"Pick" true, "Search" false}}})))
    (t/is (= [{:attribute :filter-add :var "Containers" :value [10 11]}]
             (suit/map->record {:filter-add {:containers [0xa 0xb]}})))
    (t/is (= [{:attribute :filter-all :value true}]
             (suit/map->record {:filter-all true})))))

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
             (suit/build-papyrus {:attribute :action :var "Containers" :action "Pick" :value false})))))
