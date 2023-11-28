# EDN wrapper for LooterShooter customization
## Description
While playing Starfield, dozens of items make us nervous sometimes.

Looter Shooter - Shooter Looter (https://www.nexusmods.com/starfield/mods/5294) has a massive power to rescue us from this problem, but because of it's still early era to modding, we may fall into a sea of `cgf`s to customize the behavior.

So I made this that converts from a-bit-more-human-readable EDN format to a bunch of Starfield console commands, includes hotkey declarations.

Note that the converted format is a toml (and a batch file for Papyrus engine though), so you'll also need the ConsoleCommandRunner (https://www.nexusmods.com/starfield/mods/2740).

## Requirements
- Looter Shooter - Shooter Looter: https://www.nexusmods.com/starfield/mods/5294
- ConsoleCommandRunner: https://www.nexusmods.com/starfield/mods/2740
- babashka: https://github.com/babashka/babashka

Because this script is written in Clojure language so you need a program to run, and babashka does it.

I personally recommend to install babashka via scoop on your Powershell:
```powershell
# Note: if you get an error you might need to change the execution policy (i.e. enable Powershell) with
# Set-ExecutionPolicy RemoteSigned -scope CurrentUser
Invoke-Expression (New-Object System.Net.WebClient).DownloadString('https://get.scoop.sh')

scoop bucket add scoop-clojure https://github.com/littleli/scoop-clojure
scoop bucket add extras
scoop install babashka
```

## How it works
This converter refers a file `lssl-config.edn` where LSSL config described.

EDN is similar to JSON, see also `lssl-config_example.edn` in this repository; or just remove `_example` to get started quickly.

Once you've got ready to convert, just double-click `build-toml.bat` from the Explorer and the result of settings are saved to `SFSE/Plugins/ConsoleCommandRunner/lssl.toml` as a toml format, or `Root/lssl-config.txt` as a batch file.

When you put them to the right place, now you can go get back to the Starfield with a customized LSSL!

## How to customize
See also `lssl-config_example.edn` in this repository.

### Main Control Options
`:controls` below of `:lssl-config` used to be a customization of main control options of LSSL.

In the raw console, you had to write your settings like `cgf "LSSL:Interface.SetBoolControl" "AllowStealing" false` but in EDN, `:controls {:allow-stealing false ...}` does the same.

`:value-like-this` is converted to `"ValueLikeThis"`, erasing hyphens and capitalizing the first letter of words.

Therefore, `:allow-stealing` will be converted as `"AllowStealing"` implicitly, but you can still declare like `:controls {"AllowStealing" false}` if you want.

This rule is applied over the edn.

### Filters
Just make two lists of filters, one for enabled, another one for disabled.

`:filters {:only [:custom :valuables] :except [:contraband]}` will be expanded to three lines of cgf:

- `cgf "LSSL:Interface.SetFilter" "Custom" true`
- `cgf "LSSL:Interface.SetFilter" "Valuables" true`
- `cgf "LSSL:Interface.SetFilter" "Contraband" false`

Note that the filter "EMWeap" must be formed as a string, not a keyword.

According to the capitalize-rule, `:EMWeap` will be converted to `"Emweap"` which doesn't exist.

### Filters to Ship
Similar to `:filters`, multiple `ToShip` actions can be set at a time in `:filters-to-ship`.

`:filters-to-ship [:only [:mining :flora] :except [:mines]]` generates three lines:

- `cgf "LSSL:Interface.SetFilterAction" "Mining" "ToShip" true`
- `cgf "LSSL:Interface.SetFilterAction" "Flora" "ToShip" true`
- `cgf "LSSL:Interface.SetFilterAction" "Mines" "ToShip" false`

### Actions
`:actions {:containers {:pick false :search true}}` will be expanded to two lines of cgf:

- `cgf "LSSL:Interface.SetFilterAction" "Containers" "Pick" false`
- `cgf "LSSL:Interface.SetFilterAction" "Containers" "Search" true`

Well, looks we have not so much things to discuss in this section.

## Customize deeper
EDN wrapper wraps Papyrus method calls and console commands with Vectors, things surrounded by `[` and `]`.

This makes edn appear to have a large number of brackets. Or rather, they do exist.

They have a specific form like `[function-name argument1 argument2 ...]`:

- `function-name`

starts with `:` (`:set`, `:query-state`, etc.)

- `argument1` and `argument2`

are unnecessary in some cases, depends on which function is called.

### Startup
```clojure
:startup
[[:message "LSSL launched with EDN Wrapper!"]
 [:set :pause-scan false]
 "player.additem a 1"]`
```

Codes above are transpiled to:

- `cgf "Debug.Notification" "LSSL launched with EDN Wrapper!"`
- `cgf "LSSL:Interface.SetBoolControl" "PauseScan" false`
- `player.additem a 1`

to do

1. Show a message on your HUD.
2. LSSL begins.
3. You earn 1 digipick.

`:message` and `:set` are EDN wrapper APIs and take the appropriate arguments.

Since `"player.additem a 1"` is a raw console command, it does not have to be a vector, but a string.

If you want to call `LSSL:Interface.AddToFilter`, `LSSL:Interface.AddToExclusion`, etc., put them here.

For more information about APIs, see the API section below.

### Hotkeys
```clojure
:hotkeys
{"F2"       [[:toggle-control :pause-scan]]
 "Shift-F2" [[:cancel-scan]
             [:message "Scanning cancelled."]]}
```

The above code registers the hotkey F2 to toggle scanning,
and the hotkey Shift-F2 to cancel scanning and notify the player about that.

This is the same notation as `:startup`, but lots of lines can be defined elsewhere and referenced.

```clojure
{:aliases
 {:macros
  {:close-loot  [[:set :scan-radius 5.0]   [:set :scan-timeout 60.0]]
   :normal-loot [[:set :scan-radius 25.0]  [:set :scan-timeout 60.0]]
   :super-loot  [[:set :scan-radius 250.0] [:set :scan-timeout 6000.0]]}}

 :lssl-config
 {:hotkeys
  {"Ctrl-F2" #ref [:aliases :macros :close-loot]
   "Ctrl-F3" #ref [:aliases :macros :normal-loot]
   "Ctrl-F4" #ref [:aliases :macros :super-loot]}}}
```

In the above, `:close-loot`, `:normal-loot`, and `:super-loot` are defined outside of `:lssl-config`,
and the actual `:hotkeys` refer to them using `#ref`.

For more informations about `#ref` and other features, See https://github.com/juxt/aero .

## EDN Wrapper APIs

### General Control
- `[:query-state as-control]`

calls `QueryState"`.

- `[:cancel-scan]`

calls `CancelScan"`

- `[:set :as-control value]`

`value` can be a various types, automatically converted to `SetIntControl` for Integer, `SetBoolControl` for Boolean, and so on.

- `[:mod :as-control value]`

As like as `:set`, `value` can be a various types, automatically converted to `ModIntControl` for Integer, `ModFloatControl` for Float.

- `[:toggle-control :as-control]`

calls `ToggleBoolControl`.

- `[:try-scan-now {:skip-location-exclusion boolean :skip-hand-scanner boolean}]` or `[:try-scan-now]`

calls `TryScanNow skip-location-exclusion skip-hand-scanner` or `TryScanNow false false` if no arguments.

### Filters and Actions

- `[:enable-all boolean]`

calls `SetAllFilters boolean`.

- `[:enable as-filter]`

calls `SetFilter as-filter true`.

- `[:disable as-filter]`

calls `SetFilter as-filter false`.

- `[:toggle as-filter]`

calls `ToggleFilter as-filter`.

- `[:action as-filter as-action boolean]`

calls `SetFilterAction as-filter as-action boolean`.

Note that the behavior of this API changes depending on the number of arguments.

- `[:action as-action boolean]`

calls `SetAllFiltersByAction as-action boolean`.

Note that the behavior of this API changes depending on the number of arguments.

- `[:add as-filter id]`

calls `AddToFilter as-filter id`, where id must be in hex form starts with `0x`.

For example, to add the digipick (`a`) to Custom filter,
`[:add :custom 0xa]` is valid but `[:add :custom a]` occurs an error.

- `[:remove as-filter id]`

calls `RemoveFromFilter as-filter id`.

- `[:exclude as-filter id]`

calls `AddToExclusion as-filter id`.

- `[:include as-filter id]`

calls `RemoveFromExclusion as-filter id`.

### Export Settings
- `[:export]`

calls `GenerateSettingsExport`.

### Debug
- `[:dump id]`

calls `DumpObjectInfo id`.

- `[:dump-extended]`

calls `DumpExtendedFiltersAndExclusions`.

- `[:reset float]`

calls `ResetLootedFlagInArea float`.

### Others
- `[:message string]`

calls `Debug.Notification string`.

To be exact, this is not a wrapper for the LSSL method though, it's convenient to call it easily.
