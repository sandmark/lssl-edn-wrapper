## EDN wrapper for LooterShooter customization
### Description
While playing Starfield, dozens of items make us nervous sometimes.
Looter Shooter - Shooter Looter (https://www.nexusmods.com/starfield/mods/5294) has a potential power to solve this problem, but because of it's still early era to modding, we will fall into a sea of `cgf`s to customize a behavior of the MOD.
So I made this that converts from a-bit-more-human-readable EDN format to a bunch of Starfield console commands, includes hotkeys.
Note that the converted format is a TOML, so you'll also need the ConsoleCommandRunner (https://www.nexusmods.com/starfield/mods/2740).

### Requirements
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

### How it works
This converter refers a file `lssl-config.edn` where LSSL config described.
EDN is similar to JSON, see also `lssl-config_example.edn` in this repository; or just remove `_example` to get started quickly.
Once you've got ready to convert, just double-click `build-toml.bat` from the Explorer. Then you'll see a converted toml format, which have been saved as a file `SFSE/Plugins/ConsoleCommandRunner/lssl.toml`.
When you put the file to the right place, now you can go get back to the Starfield with a powerful LSSL!

### Customization
LSSL author said:
- ScanRadius frequently changed to 10.0 or 50.0 using hotkeys, for sake of Papyrus VM's circumstances.
- NotifyOnComplete is highly recommended to set true.

Looks we've got a good example to customize, `lssl-config.edn` should be like:
```clojure
{:control
 {:scan-radius 10.0
  :notify-on-complete true}

 :hotkey
 {"F3" [:set-float-control :scan-radius 10.0]
  "F4" [:set-float-control :scan-radius 50.0]}}
```

Or you want LSSL to search but not pick containers, you can:
```clojure
{:filter
 {:containers true}

 :action
 {:containers {"Pick" false, "Search" true}}}
```

#### Hotkeys
Hotkeys are a bit complicated so let me explain more presicely.

Basically, a vector (a braced `[:like-this :one]`) will be converted to `cgf "LSSL:Interface.LikeThis One"`.
The first element `:like` is treated as a method of `LSSL:Interface`, rest of them are treated as parameters.
A kebab-case `:like-this` will be capitalized into a CamelCase `LikeThis`.

If you want to call raw `cgf` instead of as a `LSSL:Interface`'s method, just put `"string"`, not `:keyword`.
For instance, `["Debug.Notification" "Message!"]` will be converted to `cgf "Debug.Notification" "Message!"`.

A single hotkey can have multiple operations.
`LSSL:Debugger.PourAntiFreeze` and `LSSL:Interface.CancelScan` show no messages on your HUD, so you might want a notification to know whether the hotkey and operations are working correctly.
To do this, simply nest the operations with braces like:

```clojure
{:hotkey
 {"F10" [[:cancel-scan]
         ["LSSL:Debugger.PourAntiFreeze"]
         ["Debug.Notification" "Scanning canceled, pouring anti-freeze..."]]}}
```

They will be converted to `cgf`s properly and combined to one liner using a delimiter `;`.

At last, you want to call raw console commands without `cgf`s? Just replace braces with parenthesis.
```clojure
{:hotkey
 {"Shift-Y" ("player.setav speedmult 400")}}
```

I, personally, made a switch of "Field-Work" and "Exploring" with above:
```clojure
{:hotkey
 {"Y"          [["LSSL:Debugger.PourAntiFreeze"]
                [:set-bool-control :pause-scan false]
                ("player.setav speedmult 100")
                ["Debug.Notification" "Exploring mode."]]

  "Shift-Y"    [[:set-bool-control :pause-scan true]
                [:cancel-scan]
                ["LSSL:Debugger.PourAntiFreeze"]
                ("player.setav speedmult 400")
                ["Debug.Notification" "Field working mode."]]}}
```
