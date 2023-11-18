(ns lssl.processor-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lssl.processor :as sut]
   [matcher-combinators.clj-test]
   [matcher-combinators.matchers :as m]
   [clojure.string :as str]))

(deftest vector->record-test
  (testing "General Mod Control Functions"
    (testing "Query Control states"
      (is (match? {:cmd "cgf \"LSSL:Interface.QueryState\" \"General\""}
                  (sut/transpile [:query-state :general]))))
    (testing "Cancel a scan currently in progress"
      (is (match? {:cmd #"Interface\.CancelScan"}
                  (sut/transpile [:cancel-scan]))))
    (testing "Change int Control values"
      (is (match? {:cmd "cgf \"LSSL:Interface.SetIntControl\" \"AsControl\" 1"}
                  (sut/transpile [:set :as-control 1])))
      (is (match? {:cmd (m/all-of #"ModIntControl" #"-1$")}
                  (sut/transpile [:mod :as-control -1]))))
    (testing "Change float Control values"
      (is (match? {:cmd (m/all-of #"SetFloatControl" #(str/ends-with? % "1.0"))}
                  (sut/transpile [:set :as-control 1.0]))))
    (testing "Change bool Control values"
      (is (match? {:cmd (m/all-of #"SetBoolControl" #"true$")}
                  (sut/transpile [:set :as-control true])))
      (is (match? {:cmd (m/all-of #"ToggleBoolControl" #"AsControl")}
                  (sut/transpile [:toggle-control :as-control]))))
    (testing "Scan"
      (is (match? {:cmd "cgf \"LSSL:Interface.TryScanNow\" false true"}
                  (sut/transpile [:try-scan-now {:skip-hand-scanner       true
                                                 :skip-location-exclusion false}])))
      (is (match? {:cmd #"false false$"}
                  (sut/transpile [:try-scan-now])))))

  (testing "Change Filter States (enable/disable)"
    (testing "Set all Filters Actions"
      (is (match? {:cmd (m/all-of #"SetAllFilters" #(str/ends-with? % "true"))}
                  (sut/transpile [:enable-all true])))
      (is (match? {:cmd (m/all-of #"SetFilter" #"AsFilter" #"true$")}
                  (sut/transpile [:enable :as-filter])))
      (is (match? {:cmd #"EMWeap"}
                  (sut/transpile [:enable "EMWeap"])))
      (is (match? {:cmd (m/all-of #"SetFilter" #"AsFilter" #"false$")}
                  (sut/transpile [:disable :as-filter])))
      (is (match? {:cmd (m/all-of #"ToggleFilter" #"AsFilter")}
                  (sut/transpile [:toggle :as-filter]))))
    (testing "Set specific Filter Actions"
      (is (match? {:cmd "cgf \"LSSL:Interface.SetFilterAction\" \"AsFilter\" \"AsAction\" true"}
                  (sut/transpile [:action :as-filter :as-action true])))
      (is (match? {:cmd "cgf \"LSSL:Interface.SetAllFiltersByAction\" \"AsAction\" true"}
                  (sut/transpile [:action :as-action true])))))

  (testing "Add/Remove Forms and References To/From Filters and Exclusions"
    (testing "Add to Filter"
      (is (match? {:cmd "cgf \"LSSL:Interface.AddToFilter\" \"AsFilter\" a"}
                  (sut/transpile [:add :as-filter 0xa]))))
    (testing "Add to Exclusion"
      (is (match? {:cmd "cgf \"LSSL:Interface.AddToExclusion\" \"AsFilter\" f"}
                  (sut/transpile [:exclude :as-filter 0xf]))))
    (testing "Remove from Filter"
      (is (match? {:cmd "cgf \"LSSL:Interface.RemoveFromFilter\" \"AsFilter\" a"}
                  (sut/transpile [:remove :as-filter 0xa]))))
    (testing "Remove from Exclusion"
      (is (match? {:cmd "cgf \"LSSL:Interface.RemoveFromExclusion\" \"AsFilter\" f"}
                  (sut/transpile [:include :as-filter 0xf])))))

  (testing "Export Settings and Filter modifications"
    (is (match? {:cmd #"LSSL:Interface\.GenerateSettingsExport"}
                (sut/transpile [:export]))))

  (testing "Object Debugging"
    (is (match? {:cmd "cgf \"LSSL:Debugger.DumpObjectInfo\" a"}
                (sut/transpile [:dump 0xa])))
    (is (match? {:cmd #"LSSL:Debugger\.DumpExtendedFiltersAndExclusions"}
                (sut/transpile [:dump-extended])))
    (is (match? {:cmd (m/all-of #"Debugger\.ResetLootedFlagInArea" #(str/ends-with? % "50.0"))}
                (sut/transpile [:reset 50.0])))
    (is (match? {:cmd "cgf \"LSSL:Debugger.PoorAntiFreeze\""}
                (sut/transpile [:poor-anti-freeze]))))

  (testing "Papyrus Debug.Notification"
    (is (match? {:cmd "cgf \"Debug.Notification\" \"debug\""}
                (sut/transpile [:message "debug"]))))

  (testing "Raw Console Command"
    (is (match? {:cmd "tgm; tcl"}
                (sut/transpile [:raw "tgm" "tcl"])))))
